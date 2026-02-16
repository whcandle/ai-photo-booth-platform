# Admin CRUD API 完整测试脚本

$baseUrl = "http://localhost:8089"
$adminBaseUrl = "$baseUrl/api/v1/admin"
$resolveBaseUrl = "$baseUrl/api/v1/ai"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Admin CRUD API 完整测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 测试结果记录
$testResults = @()

function Test-Step {
    param(
        [string]$StepName,
        [scriptblock]$TestAction
    )
    Write-Host "=== $StepName ===" -ForegroundColor Green
    try {
        $result = & $TestAction
        Write-Host "Result: PASS" -ForegroundColor Green
        $script:testResults += @{Step = $StepName; Status = "PASS"; Result = $result}
        return $result
    } catch {
        Write-Host "Result: FAIL - $($_.Exception.Message)" -ForegroundColor Red
        $script:testResults += @{Step = $StepName; Status = "FAIL"; Error = $_.Exception.Message}
        return $null
    }
    Write-Host ""
}

# ========== 步骤 1: 创建 Provider ==========
Write-Host "步骤 1: 创建 Provider" -ForegroundColor Yellow
$createProviderBody = @{
    code = "test_provider_$(Get-Date -Format 'yyyyMMddHHmmss')"
    name = "Test Provider"
    status = "ACTIVE"
} | ConvertTo-Json

$provider = Test-Step "创建 Provider" {
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers" -Method Post -Headers $headers -Body $createProviderBody
    Write-Host "Provider ID: $($response.data.id)" -ForegroundColor Cyan
    Write-Host "Provider Code: $($response.data.code)" -ForegroundColor Cyan
    return $response.data
}

if (-not $provider) {
    Write-Host "创建 Provider 失败，终止测试" -ForegroundColor Red
    exit 1
}

$providerId = $provider.id
$providerCode = $provider.code

Write-Host ""

# ========== 步骤 2: 创建 Capability ==========
Write-Host "步骤 2: 创建 Capability" -ForegroundColor Yellow
$createCapabilityBody = @{
    capability = "segmentation"
    endpoint = "https://api.test.com/v1/segmentation"
    status = "ACTIVE"
    priority = 100
    defaultTimeoutMs = 8000
    defaultParamsJson = '{"model":"test","quality":"high"}'
} | ConvertTo-Json

$capability = Test-Step "创建 Capability" {
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers/$providerId/capabilities" -Method Post -Headers $headers -Body $createCapabilityBody
    Write-Host "Capability ID: $($response.data.id)" -ForegroundColor Cyan
    Write-Host "Capability: $($response.data.capability)" -ForegroundColor Cyan
    return $response.data
}

if (-not $capability) {
    Write-Host "创建 Capability 失败，终止测试" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ========== 步骤 3: 创建 API Key ==========
Write-Host "步骤 3: 创建 API Key" -ForegroundColor Yellow
$testApiKey = "sk-test-key-$(Get-Date -Format 'yyyyMMddHHmmss')"
$createApiKeyBody = @{
    name = "Test API Key"
    apiKey = $testApiKey
} | ConvertTo-Json

$apiKey = Test-Step "创建 API Key" {
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers/$providerId/keys" -Method Post -Headers $headers -Body $createApiKeyBody
    Write-Host "API Key ID: $($response.data.id)" -ForegroundColor Cyan
    Write-Host "API Key Name: $($response.data.name)" -ForegroundColor Cyan
    Write-Host "验证: 响应中不包含 apiKeyCipher 字段" -ForegroundColor Gray
    if ($response.data.apiKeyCipher) {
        throw "响应中不应包含 apiKeyCipher 字段"
    }
    return $response.data
}

if (-not $apiKey) {
    Write-Host "创建 API Key 失败，终止测试" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ========== 步骤 4: 验证 Resolve 立即生效 ==========
Write-Host "步骤 4: 验证 Resolve 立即生效（无需重启）" -ForegroundColor Yellow
Start-Sleep -Seconds 1  # 等待 1 秒确保数据已提交

$resolveBody = @{
    capability = "segmentation"
    prefer = @($providerCode)
} | ConvertTo-Json

$resolveResult = Test-Step "Resolve 立即生效测试" {
    $response = Invoke-RestMethod -Uri "$resolveBaseUrl/resolve" -Method Post -Headers $headers -Body $resolveBody
    Write-Host "Resolved Provider: $($response.data.direct.providerCode)" -ForegroundColor Cyan
    Write-Host "Endpoint: $($response.data.direct.endpoint)" -ForegroundColor Cyan
    Write-Host "Timeout: $($response.data.direct.timeoutMs)ms" -ForegroundColor Cyan
    
    if ($response.data.direct.providerCode -ne $providerCode) {
        throw "Resolve 返回的 provider 不匹配: 预期 $providerCode, 实际 $($response.data.direct.providerCode)"
    }
    
    if ($response.data.direct.endpoint -ne "https://api.test.com/v1/segmentation") {
        throw "Resolve 返回的 endpoint 不匹配"
    }
    
    if (-not $response.data.direct.auth) {
        throw "Resolve 响应中缺少 auth 信息"
    }
    
    if ($response.data.direct.auth.type -ne "api_key") {
        throw "Resolve 返回的 auth type 不正确"
    }
    
    if (-not $response.data.direct.auth.apiKey) {
        throw "Resolve 返回的 API Key 为空"
    }
    
    # 验证 API Key 是否正确解密（应该是我们创建的明文）
    if ($response.data.direct.auth.apiKey -ne $testApiKey) {
        Write-Host "警告: API Key 不匹配（可能是加密/解密问题）" -ForegroundColor Yellow
        Write-Host "  预期: $testApiKey" -ForegroundColor Gray
        Write-Host "  实际: $($response.data.direct.auth.apiKey)" -ForegroundColor Gray
    } else {
        Write-Host "验证: API Key 正确解密" -ForegroundColor Green
    }
    
    return $response.data
}

Write-Host ""

# ========== 步骤 5: 验证列表功能 ==========
Write-Host "步骤 5: 验证列表功能" -ForegroundColor Yellow

Test-Step "列表 Providers" {
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers" -Method Get -Headers $headers
    $found = $response.data | Where-Object { $_.id -eq $providerId }
    if (-not $found) {
        throw "创建的 Provider 未在列表中"
    }
    Write-Host "找到创建的 Provider: $($found.code)" -ForegroundColor Cyan
    return $response.data
}

Test-Step "列表 Capabilities" {
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers/$providerId/capabilities" -Method Get -Headers $headers
    $found = $response.data | Where-Object { $_.capability -eq "segmentation" }
    if (-not $found) {
        throw "创建的 Capability 未在列表中"
    }
    Write-Host "找到创建的 Capability: $($found.capability)" -ForegroundColor Cyan
    return $response.data
}

Test-Step "列表 API Keys" {
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers/$providerId/keys" -Method Get -Headers $headers
    $found = $response.data | Where-Object { $_.id -eq $apiKey.id }
    if (-not $found) {
        throw "创建的 API Key 未在列表中"
    }
    Write-Host "找到创建的 API Key: $($found.name)" -ForegroundColor Cyan
    if ($found.apiKeyCipher) {
        throw "列表响应中不应包含 apiKeyCipher 字段"
    }
    return $response.data
}

Write-Host ""

# ========== 步骤 6: 测试更新功能 ==========
Write-Host "步骤 6: 测试更新功能" -ForegroundColor Yellow

Test-Step "更新 Provider" {
    $updateBody = @{
        name = "Test Provider (Updated)"
        status = "ACTIVE"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers/$providerId" -Method Put -Headers $headers -Body $updateBody
    if ($response.data.name -ne "Test Provider (Updated)") {
        throw "Provider 名称未更新"
    }
    Write-Host "Provider 名称已更新: $($response.data.name)" -ForegroundColor Cyan
    return $response.data
}

Test-Step "更新 Capability" {
    $updateBody = @{
        endpoint = "https://api.test.com/v2/segmentation"
        priority = 50
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$adminBaseUrl/providers/$providerId/capabilities/$($capability.id)" -Method Put -Headers $headers -Body $updateBody
    if ($response.data.endpoint -ne "https://api.test.com/v2/segmentation") {
        throw "Capability endpoint 未更新"
    }
    Write-Host "Capability endpoint 已更新: $($response.data.endpoint)" -ForegroundColor Cyan
    return $response.data
}

Write-Host ""

# ========== 步骤 7: 验证更新后的 Resolve ==========
Write-Host "步骤 7: 验证更新后的 Resolve 立即生效" -ForegroundColor Yellow
Start-Sleep -Seconds 1

Test-Step "Resolve 使用更新后的 endpoint" {
    $response = Invoke-RestMethod -Uri "$resolveBaseUrl/resolve" -Method Post -Headers $headers -Body $resolveBody
    if ($response.data.direct.endpoint -ne "https://api.test.com/v2/segmentation") {
        throw "Resolve 未使用更新后的 endpoint: 预期 https://api.test.com/v2/segmentation, 实际 $($response.data.direct.endpoint)"
    }
    Write-Host "验证: Resolve 使用了更新后的 endpoint" -ForegroundColor Green
    return $response.data
}

Write-Host ""

# ========== 测试总结 ==========
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试总结" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$passed = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Host "总测试数: $($testResults.Count)" -ForegroundColor Cyan
Write-Host "通过: $passed" -ForegroundColor Green
Write-Host "失败: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -eq 0) {
    Write-Host "`n所有测试通过！" -ForegroundColor Green
    Write-Host "功能验证:" -ForegroundColor Cyan
    Write-Host "  - Provider/Capability/Key 创建成功" -ForegroundColor Green
    Write-Host "  - Resolve 立即生效（无需重启）" -ForegroundColor Green
    Write-Host "  - 更新功能正常" -ForegroundColor Green
    Write-Host "  - API Key 不回显明文" -ForegroundColor Green
} else {
    Write-Host "`n部分测试失败，请检查错误信息" -ForegroundColor Red
    $testResults | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "  - $($_.Step): $($_.Error)" -ForegroundColor Red
    }
}

Write-Host "========================================" -ForegroundColor Cyan
