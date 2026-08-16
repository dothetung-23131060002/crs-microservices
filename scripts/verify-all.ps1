param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$DbPassword = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "12345678" }),
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$verificationRoot = Join-Path $repoRoot "verification"
$reportRoot = Join-Path $verificationRoot "reports"
$logRoot = Join-Path $verificationRoot "logs"
$serviceProcesses = @{}

New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
New-Item -ItemType Directory -Path $logRoot -Force | Out-Null

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw "JAVA_HOME is not configured."
}

$javaExe = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "java.exe was not found under JAVA_HOME: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
$env:Path = (Join-Path $JavaHome "bin") + ";" + $env:Path

$services = @(
    [PSCustomObject]@{ Name = "auth-service"; Port = 8081; Jar = "auth-service-0.0.1-SNAPSHOT.jar" },
    [PSCustomObject]@{ Name = "course-service"; Port = 8082; Jar = "course-service-0.0.1-SNAPSHOT.jar" },
    [PSCustomObject]@{ Name = "registration-service"; Port = 8083; Jar = "registration-service-0.0.1-SNAPSHOT.jar" },
    [PSCustomObject]@{ Name = "api-gateway"; Port = 8080; Jar = "api-gateway-0.0.1-SNAPSHOT.jar" }
)

function Assert-ExitCode {
    param([string]$Action)

    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE."
    }
}

function Wait-ForPort {
    param(
        [int]$Port,
        [bool]$Listening,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $isListening = [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
        if ($isListening -eq $Listening) {
            return
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)

    throw "Port $Port did not reach Listening=$Listening within $TimeoutSeconds seconds."
}

function Start-LabService {
    param([PSCustomObject]$Service)

    $serviceRoot = Join-Path $repoRoot $Service.Name
    $jarRelativePath = Join-Path "target" $Service.Jar
    $jarAbsolutePath = Join-Path $serviceRoot $jarRelativePath
    if (-not (Test-Path -LiteralPath $jarAbsolutePath)) {
        throw "Missing executable jar: $jarAbsolutePath"
    }

    $stdoutPath = Join-Path $logRoot ($Service.Name + ".stdout.log")
    $stderrPath = Join-Path $logRoot ($Service.Name + ".stderr.log")
    $process = Start-Process `
        -FilePath $javaExe `
        -ArgumentList "-jar", $jarRelativePath `
        -WorkingDirectory $serviceRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    $script:serviceProcesses[$Service.Name] = $process
}

function Stop-LabService {
    param([string]$Name)

    if (-not $script:serviceProcesses.ContainsKey($Name)) {
        return
    }

    $process = $script:serviceProcesses[$Name]
    if ($null -ne $process -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
        $process.WaitForExit(10000) | Out-Null
    }
}

function Invoke-NewmanCollection {
    param(
        [string]$Name,
        [string]$CollectionFile
    )

    if (-not (Test-Path -LiteralPath $CollectionFile)) {
        throw "Missing Postman collection: $CollectionFile"
    }

    $junitPath = Join-Path $reportRoot ($Name + ".xml")
    $cliPath = Join-Path $reportRoot ($Name + ".txt")
    $previousErrorActionPreference = $ErrorActionPreference
    $previousNodeNoWarnings = $env:NODE_NO_WARNINGS
    try {
        # npm/newman can emit harmless deprecation warnings on stderr. PowerShell 5
        # must not turn those warnings into terminating ErrorRecord instances.
        $ErrorActionPreference = "Continue"
        $env:NODE_NO_WARNINGS = "1"
        & npx.cmd --yes newman@6.2.2 run $CollectionFile `
            --reporters cli,junit `
            --reporter-cli-show-timestamps `
            --reporter-junit-export $junitPath 2>&1 | Tee-Object -FilePath $cliPath
        $newmanExitCode = $LASTEXITCODE
    }
    finally {
        $env:NODE_NO_WARNINGS = $previousNodeNoWarnings
        $ErrorActionPreference = $previousErrorActionPreference
    }

    # Windows PowerShell 5 writes Tee-Object files as UTF-16LE. Normalize the
    # generated CLI artifact to UTF-8 so Git and text viewers treat it as text.
    if (Test-Path -LiteralPath $cliPath) {
        $cliOutput = Get-Content -LiteralPath $cliPath
        $cliOutput | Set-Content -LiteralPath $cliPath -Encoding UTF8
    }

    if ($newmanExitCode -ne 0) {
        throw "Newman collection '$Name' failed with exit code $newmanExitCode."
    }
}

function Get-MavenTestEvidence {
    $serviceResults = @()
    $totalTests = 0
    $totalFailures = 0
    $totalErrors = 0
    $totalSkipped = 0

    foreach ($service in $services) {
        $surefireRoot = Join-Path $repoRoot ($service.Name + "\target\surefire-reports")
        $reportFiles = @(Get-ChildItem `
            -LiteralPath $surefireRoot `
            -Filter "TEST-*.xml" `
            -ErrorAction SilentlyContinue)
        if ($reportFiles.Count -eq 0) {
            throw "No Maven Surefire reports were found for $($service.Name)."
        }

        $serviceTests = 0
        $serviceFailures = 0
        $serviceErrors = 0
        $serviceSkipped = 0
        foreach ($reportFile in $reportFiles) {
            [xml]$report = Get-Content -LiteralPath $reportFile.FullName -Raw
            $serviceTests += [int]$report.testsuite.tests
            $serviceFailures += [int]$report.testsuite.failures
            $serviceErrors += [int]$report.testsuite.errors
            $serviceSkipped += [int]$report.testsuite.skipped
        }

        $serviceResults += [PSCustomObject]@{
            service = $service.Name
            tests = $serviceTests
            failures = $serviceFailures
            errors = $serviceErrors
            skipped = $serviceSkipped
        }
        $totalTests += $serviceTests
        $totalFailures += $serviceFailures
        $totalErrors += $serviceErrors
        $totalSkipped += $serviceSkipped
    }

    $evidence = [PSCustomObject]@{
        checkedAt = (Get-Date).ToString("o")
        tests = $totalTests
        failures = $totalFailures
        errors = $totalErrors
        skipped = $totalSkipped
        services = $serviceResults
    }
    $evidence | ConvertTo-Json -Depth 4 | Set-Content `
        -LiteralPath (Join-Path $reportRoot "maven-tests.json") `
        -Encoding UTF8

    if ($totalTests -ne 41 -or $totalFailures -ne 0 -or $totalErrors -ne 0 -or $totalSkipped -ne 0) {
        throw "Unexpected Maven test totals: tests=$totalTests, failures=$totalFailures, errors=$totalErrors, skipped=$totalSkipped."
    }

    return $evidence
}

function Get-MySqlExecutable {
    $mysqlCandidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    )
    $mysqlExe = $mysqlCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $mysqlExe) {
        throw "MySQL CLI was not found."
    }
    return $mysqlExe
}

function Invoke-MySqlQuery {
    param([string]$Query)

    $mysqlExe = Get-MySqlExecutable
    $oldMysqlPassword = $env:MYSQL_PWD
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $env:MYSQL_PWD = $DbPassword
        $ErrorActionPreference = "Continue"
        $queryOutput = @(& $mysqlExe `
            --user=root `
            --host=localhost `
            --port=3306 `
            --batch `
            --raw `
            --skip-column-names `
            --execute=$Query 2>&1)
        $mysqlExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
        $env:MYSQL_PWD = $oldMysqlPassword
    }
    if ($mysqlExitCode -ne 0) {
        throw "MySQL verification query failed with exit code $mysqlExitCode."
    }
    return $queryOutput
}

function Test-CourseServiceUnavailable {
    Stop-LabService "course-service"
    Wait-ForPort -Port 8082 -Listening $false -TimeoutSeconds 30

    $loginBody = @{ username = "student1"; password = "student123" } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod `
        -Uri "http://localhost:8081/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody

    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    try {
        $request = New-Object System.Net.Http.HttpRequestMessage(
            [System.Net.Http.HttpMethod]::Post,
            "http://localhost:8083/registrations"
        )
        $request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue(
            "Bearer",
            $login.token
        )
        $uniqueStudentId = 900000000 + (Get-Random -Minimum 1 -Maximum 99999999)
        $requestBody = @{ studentId = $uniqueStudentId; courseId = 999999999 } | ConvertTo-Json -Compress
        $request.Content = New-Object System.Net.Http.StringContent(
            $requestBody,
            [System.Text.Encoding]::UTF8,
            "application/json"
        )

        $timer = [System.Diagnostics.Stopwatch]::StartNew()
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $timer.Stop()
        $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

        if ([int]$response.StatusCode -ne 503) {
            throw "Expected HTTP 503 with course-service stopped, received $([int]$response.StatusCode)."
        }
        if ($timer.ElapsedMilliseconds -gt 6500) {
            throw "Unavailable response exceeded 6.5 seconds: $($timer.ElapsedMilliseconds) ms."
        }
        if ($responseBody -notmatch "Khong the ket noi toi course-service") {
            throw "Unavailable response did not contain the expected message: $responseBody"
        }

        [PSCustomObject]@{
            checkedAt = (Get-Date).ToString("o")
            courseServicePort8082Listening = $false
            status = [int]$response.StatusCode
            elapsedMilliseconds = $timer.ElapsedMilliseconds
            message = (($responseBody | ConvertFrom-Json).message)
        } | ConvertTo-Json | Set-Content `
            -LiteralPath (Join-Path $reportRoot "course-service-unavailable.json") `
            -Encoding UTF8
    }
    finally {
        $client.Dispose()
    }
}

function Write-DatabaseEvidence {
    $query = @"
SELECT schema_name FROM information_schema.schemata WHERE schema_name IN ('auth_db','course_db','registration_db') ORDER BY schema_name;
SELECT CONCAT('course_count=', COUNT(*)) FROM course_db.course;
SELECT CONCAT('registration_count=', COUNT(*)) FROM registration_db.registration;
SELECT CONCAT(username, '|', role, '|bcrypt=', password LIKE '`$2%') FROM auth_db.app_user ORDER BY username;
"@
    Invoke-MySqlQuery -Query $query | Set-Content `
        -LiteralPath (Join-Path $reportRoot "database-evidence.txt") `
        -Encoding UTF8
}

function Write-DatabaseTransitionEvidence {
    $runId = [Guid]::NewGuid().ToString("N")
    $courseName = "SQL evidence $runId"
    $updatedCourseName = "$courseName updated"
    $studentId = 700000000 + (Get-Random -Minimum 1 -Maximum 99999999)
    $courseId = $null
    $registrationId = $null
    $registrationCancelled = $false
    $evidenceLines = @(
        "checkedAt=$((Get-Date).ToString('o'))",
        "runId=$runId"
    )

    $adminLoginBody = @{ username = "admin"; password = "admin123" } | ConvertTo-Json -Compress
    $studentLoginBody = @{ username = "student1"; password = "student123" } | ConvertTo-Json -Compress
    $adminLogin = Invoke-RestMethod `
        -Uri "http://localhost:8081/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $adminLoginBody
    $studentLogin = Invoke-RestMethod `
        -Uri "http://localhost:8081/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $studentLoginBody
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.token)" }
    $studentHeaders = @{ Authorization = "Bearer $($studentLogin.token)" }

    try {
        $courseBody = @{
            tenMonHoc = $courseName
            soTinChi = 3
            soChoToiDa = 1
        } | ConvertTo-Json -Compress
        $course = Invoke-RestMethod `
            -Uri "http://localhost:8082/courses" `
            -Method Post `
            -Headers $adminHeaders `
            -ContentType "application/json" `
            -Body $courseBody
        $courseId = [long]$course.id
        $evidenceLines += Invoke-MySqlQuery -Query @"
SELECT CONCAT('after_create|course_id=', id, '|name=', ten_mon_hoc, '|max=', so_cho_toi_da, '|remaining=', so_cho_con_lai)
FROM course_db.course WHERE id = $courseId;
"@

        $updateBody = @{
            tenMonHoc = $updatedCourseName
            soTinChi = 4
            soChoToiDa = 1
        } | ConvertTo-Json -Compress
        Invoke-RestMethod `
            -Uri "http://localhost:8082/courses/$courseId" `
            -Method Put `
            -Headers $adminHeaders `
            -ContentType "application/json" `
            -Body $updateBody | Out-Null
        $evidenceLines += Invoke-MySqlQuery -Query @"
SELECT CONCAT('after_update|course_id=', id, '|name=', ten_mon_hoc, '|credits=', so_tin_chi, '|remaining=', so_cho_con_lai)
FROM course_db.course WHERE id = $courseId;
"@

        $registrationBody = @{
            studentId = $studentId
            courseId = $courseId
        } | ConvertTo-Json -Compress
        $registration = Invoke-RestMethod `
            -Uri "http://localhost:8083/registrations" `
            -Method Post `
            -Headers $studentHeaders `
            -ContentType "application/json" `
            -Body $registrationBody
        $registrationId = [long]$registration.id
        $evidenceLines += Invoke-MySqlQuery -Query @"
SELECT CONCAT('after_registration|course_id=', c.id, '|remaining=', c.so_cho_con_lai,
              '|registration_id=', r.id, '|student_id=', r.student_id, '|status=', r.trang_thai)
FROM course_db.course c
JOIN registration_db.registration r ON r.course_id = c.id
WHERE c.id = $courseId AND r.id = $registrationId;
"@

        Invoke-RestMethod `
            -Uri "http://localhost:8083/registrations/$registrationId" `
            -Method Delete `
            -Headers $studentHeaders | Out-Null
        $registrationCancelled = $true
        $evidenceLines += Invoke-MySqlQuery -Query @"
SELECT CONCAT('after_cancellation|course_id=', c.id, '|remaining=', c.so_cho_con_lai,
              '|registration_id=', r.id, '|status=', r.trang_thai)
FROM course_db.course c
JOIN registration_db.registration r ON r.course_id = c.id
WHERE c.id = $courseId AND r.id = $registrationId;
"@

        Invoke-RestMethod `
            -Uri "http://localhost:8082/courses/$courseId" `
            -Method Delete `
            -Headers $adminHeaders | Out-Null
        $evidenceLines += Invoke-MySqlQuery -Query @"
SELECT CONCAT('after_delete|course_id=$courseId|row_count=', COUNT(*))
FROM course_db.course WHERE id = $courseId;
"@
        $courseId = $null
    }
    finally {
        if ($null -ne $registrationId -and -not $registrationCancelled) {
            try {
                Invoke-RestMethod `
                    -Uri "http://localhost:8083/registrations/$registrationId" `
                    -Method Delete `
                    -Headers $studentHeaders | Out-Null
            }
            catch {
                Write-Warning "Could not cancel verification registration $registrationId during cleanup."
            }
        }
        if ($null -ne $courseId) {
            try {
                Invoke-RestMethod `
                    -Uri "http://localhost:8082/courses/$courseId" `
                    -Method Delete `
                    -Headers $adminHeaders | Out-Null
            }
            catch {
                Write-Warning "Could not delete verification course $courseId during cleanup."
            }
        }
    }

    $evidenceLines | Set-Content `
        -LiteralPath (Join-Path $reportRoot "database-transitions.txt") `
        -Encoding UTF8
}

try {
    $occupiedPorts = Get-NetTCPConnection `
        -State Listen `
        -LocalPort ($services.Port) `
        -ErrorAction SilentlyContinue
    if ($occupiedPorts) {
        $occupied = ($occupiedPorts | Select-Object -ExpandProperty LocalPort | Sort-Object -Unique) -join ", "
        throw "Required ports are already in use: $occupied"
    }

    if (-not $SkipBuild) {
        foreach ($service in $services) {
            $serviceRoot = Join-Path $repoRoot $service.Name
            $serviceWrapper = Join-Path $serviceRoot "mvnw.cmd"
            $wrapperFromServiceRoot = if (Test-Path -LiteralPath $serviceWrapper) {
                ".\mvnw.cmd"
            }
            else {
                "..\course-service\mvnw.cmd"
            }

            Push-Location $serviceRoot
            try {
                & $wrapperFromServiceRoot -q clean package
                Assert-ExitCode "Maven package for $($service.Name)"
            }
            finally {
                Pop-Location
            }
        }
    }

    $mavenEvidence = Get-MavenTestEvidence

    foreach ($service in $services) {
        Start-LabService $service
    }
    foreach ($service in $services) {
        Wait-ForPort -Port $service.Port -Listening $true
    }

    Invoke-NewmanCollection `
        -Name "auth-service" `
        -CollectionFile (Join-Path $repoRoot "postman\auth-service.postman_collection.json")
    Invoke-NewmanCollection `
        -Name "course-service" `
        -CollectionFile (Join-Path $repoRoot "postman\course-service.postman_collection.json")
    Invoke-NewmanCollection `
        -Name "registration-service" `
        -CollectionFile (Join-Path $repoRoot "postman\registration-service.postman_collection.json")
    Invoke-NewmanCollection `
        -Name "api-gateway" `
        -CollectionFile (Join-Path $repoRoot "postman\CRS-Microservices.postman_collection.json")

    Write-DatabaseTransitionEvidence
    Write-DatabaseEvidence
    Test-CourseServiceUnavailable

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $javaVersionOutput = @(& $javaExe -version 2>&1)
        $javaExitCode = $LASTEXITCODE
        $javacVersionOutput = @(& (Join-Path $JavaHome "bin\javac.exe") -version 2>&1)
        $javacExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($javaExitCode -ne 0 -or $javacExitCode -ne 0) {
        throw "Java toolchain verification failed."
    }
    $javaVersion = ($javaVersionOutput | Select-Object -First 1).ToString()
    $javacVersion = ($javacVersionOutput | Select-Object -First 1).ToString()

    [PSCustomObject]@{
        checkedAt = (Get-Date).ToString("o")
        java = $javaVersion
        javac = $javacVersion
        mavenTests = $mavenEvidence.tests
        newmanCollections = 4
        databaseTransitionChecks = 5
        status = "PASS"
    } | ConvertTo-Json | Set-Content `
        -LiteralPath (Join-Path $reportRoot "verification-summary.json") `
        -Encoding UTF8

    Write-Host "All four lab verification suites passed."
}
finally {
    foreach ($service in $services) {
        Stop-LabService $service.Name
    }
}
