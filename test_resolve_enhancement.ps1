# ResolveService 增强功能测试脚本

$baseUrl = "http://localhost:8089/api/v1/ai/resolve"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ResolveService 增强功能测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 测试 1: Global Policy 生效
Write-Host "=== 测试 1: Global Policy 生效 ===" -ForegroundColor Green
$body1 = @{
    capability = "segmentation"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body1
    Write-Host "Provider Code: $($response1.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "预期: aliyun (GLOBAL policy 第一个)" -ForegroundColor Gray
    if ($response1.data.direct.providerCode -eq "aliyun") {
        Write-Host "结果: PASS" -ForegroundColor Green
    } else {
        Write-Host "结果: FAIL (预期 aliyun，实际 $($response1.data.direct.providerCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 测试 2: Request Prefer 覆盖 Policy
Write-Host "=== 测试 2: Request Prefer 覆盖 Policy ===" -ForegroundColor Green
$body2 = @{
    capability = "segmentation"
    prefer = @("volc", "aliyun")
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body2
    Write-Host "Provider Code: $($response2.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "预期: volc (请求 prefer 第一个)" -ForegroundColor Gray
    if ($response2.data.direct.providerCode -eq "volc") {
        Write-Host "结果: PASS" -ForegroundColor Green
    } else {
        Write-Host "结果: FAIL (预期 volc，实际 $($response2.data.direct.providerCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 测试 3: Merchant Policy 生效
Write-Host "=== 测试 3: Merchant Policy 生效 ===" -ForegroundColor Green
$body3 = @{
    capability = "segmentation"
    merchantCode = "TEST001"
} | ConvertTo-Json

try {
    $response3 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body3
    Write-Host "Provider Code: $($response3.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "预期: volc (MERCHANT policy 第一个)" -ForegroundColor Gray
    if ($response3.data.direct.providerCode -eq "volc") {
        Write-Host "结果: PASS" -ForegroundColor Green
    } else {
        Write-Host "结果: FAIL (预期 volc，实际 $($response3.data.direct.providerCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 测试 4: 优先级完整测试 - 请求 prefer 优先
Write-Host "=== 测试 4: 优先级测试 - 请求 prefer 优先 ===" -ForegroundColor Green
$body4 = @{
    capability = "segmentation"
    merchantCode = "TEST001"
    prefer = @("local_sd")
} | ConvertTo-Json

try {
    $response4 = Invoke-RestMethod -Uri $baseUrl -Method Post -Headers $headers -Body $body4
    Write-Host "Provider Code: $($response4.data.direct.providerCode)" -ForegroundColor Yellow
    Write-Host "预期: local_sd (请求 prefer 最高优先级)" -ForegroundColor Gray
    if ($response4.data.direct.providerCode -eq "local_sd") {
        Write-Host "结果: PASS" -ForegroundColor Green
    } else {
        Write-Host "结果: FAIL (预期 local_sd，实际 $($response4.data.direct.providerCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
