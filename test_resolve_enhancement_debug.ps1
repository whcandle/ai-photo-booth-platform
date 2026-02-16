# ResolveService Enhancement Test Script (Debug Version)

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ResolveService Enhancement Test (Debug)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Global Policy
Write-Host "=== Test 1: Global Policy ===" -ForegroundColor Green
$body1 = @{
    capability = "segmentation"
} | ConvertTo-Json

Write-Host "Request Body: $body1" -ForegroundColor Gray
try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body1
    Write-Host "Provider Code: $($response1.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "Expected: aliyun (GLOBAL policy first)" -ForegroundColor Gray
    if ($response1.data.direct.providerCode -eq "aliyun") {
        Write-Host "Result: PASS" -ForegroundColor Green
    } else {
        Write-Host "Result: FAIL (Expected aliyun, got $($response1.data.direct.providerCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $errorStream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorStream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Response: $errorBody" -ForegroundColor Red
        $errorJson = $errorBody | ConvertFrom-Json
        Write-Host "Error Message: $($errorJson.message)" -ForegroundColor Red
    } catch {
        Write-Host "Cannot parse error response" -ForegroundColor Red
    }
}

Write-Host ""

# Test 2: Request Prefer Override Policy
Write-Host "=== Test 2: Request Prefer Override Policy ===" -ForegroundColor Green
$body2 = @{
    capability = "segmentation"
    prefer = @("volc", "aliyun")
} | ConvertTo-Json

Write-Host "Request Body: $body2" -ForegroundColor Gray
try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body2
    Write-Host "Provider Code: $($response2.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "Result: PASS" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $errorStream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorStream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Response: $errorBody" -ForegroundColor Red
        $errorJson = $errorBody | ConvertFrom-Json
        Write-Host "Error Message: $($errorJson.message)" -ForegroundColor Red
    } catch {
        Write-Host "Cannot parse error response" -ForegroundColor Red
    }
}

Write-Host ""

# Test 3: Merchant Policy
Write-Host "=== Test 3: Merchant Policy ===" -ForegroundColor Green
$body3 = @{
    capability = "segmentation"
    merchantCode = "TEST001"
} | ConvertTo-Json

Write-Host "Request Body: $body3" -ForegroundColor Gray
try {
    $response3 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body3
    Write-Host "Provider Code: $($response3.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "Expected: volc (MERCHANT policy first)" -ForegroundColor Gray
    if ($response3.data.direct.providerCode -eq "volc") {
        Write-Host "Result: PASS" -ForegroundColor Green
    } else {
        Write-Host "Result: FAIL (Expected volc, got $($response3.data.direct.providerCode))" -ForegroundColor Red
        Write-Host "Possible reason: MERCHANT policy not found or merchantCode incorrect" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $errorStream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorStream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Response: $errorBody" -ForegroundColor Red
    } catch {
        Write-Host "Cannot parse error response" -ForegroundColor Red
    }
}

Write-Host ""

# Test 4: Check Available Providers
Write-Host "=== Test 4: Check Available Providers ===" -ForegroundColor Green
$body4 = @{
    capability = "segmentation"
} | ConvertTo-Json

Write-Host "Request Body: $body4" -ForegroundColor Gray
try {
    $response4 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body4
    Write-Host "Current Provider: $($response4.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "Endpoint: $($response4.data.direct.endpoint)" -ForegroundColor Yellow
    Write-Host "Result: PASS" -ForegroundColor Green
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $errorStream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorStream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Response: $errorBody" -ForegroundColor Red
        $errorJson = $errorBody | ConvertFrom-Json
        Write-Host "Error Message: $($errorJson.message)" -ForegroundColor Red
    } catch {
        Write-Host "Cannot parse error response" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Debug Information" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "If tests fail, please check:" -ForegroundColor Yellow
Write-Host "1. Whether provider_capabilities exist in database" -ForegroundColor Gray
Write-Host "2. Whether provider_api_keys exist (status='ACTIVE')" -ForegroundColor Gray
Write-Host "3. Whether MERCHANT policy is correctly configured (merchant_id and capability)" -ForegroundColor Gray
Write-Host "4. Whether requested prefer providers exist" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
