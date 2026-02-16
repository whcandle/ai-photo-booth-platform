# Test script for verifying updatedAt field in templates API
# Usage: .\test_templates_updated_at.ps1 -deviceId <deviceId> -activityId <activityId> -token <deviceToken>

param(
    [Parameter(Mandatory=$true)]
    [Long]$deviceId,
    
    [Parameter(Mandatory=$true)]
    [Long]$activityId,
    
    [Parameter(Mandatory=$true)]
    [String]$token,
    
    [String]$baseUrl = "http://127.0.0.1:8089"
)

Write-Host "=== Testing Templates API with updatedAt field ===" -ForegroundColor Cyan
Write-Host ""

$url = "$baseUrl/api/v1/device/$deviceId/activities/$activityId/templates"
Write-Host "Request URL: $url" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    Write-Host "Sending GET request..." -ForegroundColor Yellow
    $response = Invoke-WebRequest -Uri $url -Method GET -Headers $headers -UseBasicParsing -ErrorAction Stop
    
    Write-Host "[OK] Request successful (Status: $($response.StatusCode))" -ForegroundColor Green
    Write-Host ""
    
    $json = $response.Content | ConvertFrom-Json
    
    if ($json.success -eq $true) {
        Write-Host "[OK] API returned success=true" -ForegroundColor Green
        Write-Host ""
        
        $templates = $json.data
        $count = $templates.Count
        
        Write-Host "Found $count template(s):" -ForegroundColor Cyan
        Write-Host ""
        
        $allHaveUpdatedAt = $true
        $index = 1
        
        foreach ($template in $templates) {
            Write-Host "Template #$index :" -ForegroundColor Yellow
            Write-Host "  templateId: $($template.templateId)" -ForegroundColor Gray
            Write-Host "  name: $($template.name)" -ForegroundColor Gray
            Write-Host "  version: $($template.version)" -ForegroundColor Gray
            Write-Host "  enabled: $($template.enabled)" -ForegroundColor Gray
            
            if ($template.PSObject.Properties.Name -contains "updatedAt") {
                if ($null -ne $template.updatedAt -and $template.updatedAt -ne "") {
                    Write-Host "  updatedAt: $($template.updatedAt) [OK]" -ForegroundColor Green
                    
                    # Validate ISO8601 format (basic check)
                    if ($template.updatedAt -match "^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}") {
                        Write-Host "    Format: ISO8601 (valid)" -ForegroundColor Green
                    } else {
                        Write-Host "    Format: WARNING - may not be ISO8601" -ForegroundColor Yellow
                    }
                } else {
                    Write-Host "  updatedAt: null or empty [WARN]" -ForegroundColor Yellow
                    $allHaveUpdatedAt = $false
                }
            } else {
                Write-Host "  updatedAt: FIELD MISSING [ERROR]" -ForegroundColor Red
                $allHaveUpdatedAt = $false
            }
            Write-Host ""
            $index++
        }
        
        Write-Host "=== Summary ===" -ForegroundColor Cyan
        if ($allHaveUpdatedAt) {
            Write-Host "[OK] All templates have updatedAt field" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Some templates are missing updatedAt or it is null" -ForegroundColor Yellow
        }
        Write-Host ""
        Write-Host "Full JSON response:" -ForegroundColor Cyan
        $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
        
    } else {
        Write-Host "[ERROR] API returned success=false" -ForegroundColor Red
        Write-Host "Message: $($json.message)" -ForegroundColor Red
        exit 1
    }
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "[ERROR] Request failed (Status: $statusCode)" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    
    if ($statusCode -eq 401) {
        Write-Host ""
        Write-Host "Hint: Check your device token. It may be expired or invalid." -ForegroundColor Yellow
    } elseif ($statusCode -eq 403) {
        Write-Host ""
        Write-Host "Hint: Device may not have access to this activity." -ForegroundColor Yellow
    }
    
    exit 1
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan
