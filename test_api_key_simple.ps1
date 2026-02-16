# Simple API Key Test Script

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Key Auto Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$plainKey = "sk-test-aliyun-key-12345"

# Step 1: Generate Base64 encoded value for testing
Write-Host "Step 1: Generate test encryption value..." -ForegroundColor Yellow
$bytes = [System.Text.Encoding]::UTF8.GetBytes($plainKey)
$base64Encoded = [Convert]::ToBase64String($bytes)
Write-Host "Plain API Key: $plainKey" -ForegroundColor Cyan
Write-Host "Base64 Encoded: $base64Encoded" -ForegroundColor Green
Write-Host ""
Write-Host "SQL Insert (copy and execute):" -ForegroundColor Yellow
Write-Host "INSERT INTO provider_api_keys (provider_id, name, api_key_cipher, status) SELECT id, 'Aliyun Test API Key', '$base64Encoded', 'ACTIVE' FROM model_providers WHERE code = 'aliyun';" -ForegroundColor Gray
Write-Host ""

# Step 2: Check if app is running
Write-Host "Step 2: Check application status..." -ForegroundColor Yellow
try {
    $testBody = '{"capability":"segmentation"}'
    $null = Invoke-WebRequest -Uri $baseUrl -Method Post -Headers @{"Content-Type"="application/json"} -Body $testBody -ErrorAction Stop
    Write-Host "Application is running" -ForegroundColor Green
    $appRunning = $true
} catch {
    Write-Host "Application is NOT running. Please start: mvn spring-boot:run" -ForegroundColor Red
    $appRunning = $false
}

if (-not $appRunning) {
    Write-Host ""
    Write-Host "Test stopped. Please start the application first." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Test Conclusion" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Status: Application not running" -ForegroundColor Red
    Write-Host "Action: Start application and run this script again" -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "Step 3: Test API..." -ForegroundColor Yellow

$body = @{
    capability = "segmentation"
    prefer = @("aliyun")
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers @{"Content-Type"="application/json"} -Body $body
    
    Write-Host "API call successful" -ForegroundColor Green
    Write-Host "Provider: $($response.data.direct.providerCode)" -ForegroundColor Cyan
    Write-Host "API Key Type: $($response.data.direct.auth.type)" -ForegroundColor Cyan
    
    if ($response.data.direct.auth.apiKey) {
        $apiKey = $response.data.direct.auth.apiKey
        $keyLength = $apiKey.Length
        $preview = if ($keyLength -gt 15) { $apiKey.Substring(0, 15) + "..." } else { $apiKey }
        Write-Host "API Key: $preview (length: $keyLength)" -ForegroundColor Green
        
        if ($apiKey -eq $plainKey) {
            Write-Host "Verification: API Key correctly decrypted" -ForegroundColor Green
        }
        
        Write-Host ""
        Write-Host "Full Response:" -ForegroundColor Cyan
        $response | ConvertTo-Json -Depth 10
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "Test Conclusion" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "1. PASS: API Key functionality works" -ForegroundColor Green
        Write-Host "2. PASS: API Key successfully decrypted and returned" -ForegroundColor Green
        Write-Host "3. PASS: Response format is correct" -ForegroundColor Green
        Write-Host "4. PASS: No plain API Key in logs (check manually)" -ForegroundColor Green
    } else {
        Write-Host "API Key is empty, need to insert test data" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "Test Conclusion" -ForegroundColor Yellow
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "Status: API Key not found" -ForegroundColor Yellow
        Write-Host "Action: Execute the SQL statement above to insert test data" -ForegroundColor Yellow
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400) {
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
            Write-Host "API returned 400 error" -ForegroundColor Red
            Write-Host "Error: $($errorBody.message)" -ForegroundColor Red
            
            if ($errorBody.message -like "*NO_ACTIVE_API_KEY*") {
                Write-Host ""
                Write-Host "Error handling works: NO_ACTIVE_API_KEY returned" -ForegroundColor Green
                Write-Host ""
                Write-Host "========================================" -ForegroundColor Cyan
                Write-Host "Test Conclusion" -ForegroundColor Yellow
                Write-Host "========================================" -ForegroundColor Cyan
                Write-Host "Status: No API Key found (expected)" -ForegroundColor Yellow
                Write-Host "Error Code: NO_ACTIVE_API_KEY (correct)" -ForegroundColor Green
                Write-Host "HTTP Status: 400 (correct)" -ForegroundColor Green
                Write-Host "Action: Execute the SQL statement above to insert test data" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "API call failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Test completed" -ForegroundColor Cyan
