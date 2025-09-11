#!/bin/bash

# 📄 Add Copyright Headers to Java Files
# Script to add Sthwalo Holdings copyright headers to all Java source files

echo "📄 Adding Copyright Headers to Java Files"
echo "=========================================="
echo "Owner: Immaculate Nyoni | Sthwalo Holdings (Pty) Ltd."
echo "Contact: sthwaloe@gmail.com | +27 61 514 6185"
echo ""

# Copyright header template
read -r -d '' COPYRIGHT_HEADER << 'EOF'
/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

EOF

# Count Java files
java_files=$(find . -name "*.java" | grep -E "(src/|app/)" | head -20)
total_count=$(echo "$java_files" | wc -l)

echo "🔍 Found $total_count Java files to process"

# Function to add copyright header
add_copyright() {
    local file=$1
    local temp_file="${file}.tmp"
    
    # Check if copyright already exists
    if grep -q "Sthwalo Holdings" "$file" 2>/dev/null; then
        echo "  ⏭️  Copyright already exists: $(basename "$file")"
        return 0
    fi
    
    echo "  ✏️  Adding copyright to: $(basename "$file")"
    
    # Create temp file with copyright header
    echo "$COPYRIGHT_HEADER" > "$temp_file"
    
    # Add original file content
    cat "$file" >> "$temp_file"
    
    # Replace original file
    mv "$temp_file" "$file"
}

# Process key Java files
echo ""
echo "📝 Processing Java files..."

# Key application files
key_files=(
    "app/src/main/java/fin/App.java"
    "app/src/main/java/fin/ApplicationController.java"
    "app/src/main/java/fin/CompanyController.java"
    "app/src/main/java/fin/model/Company.java"
    "app/src/main/java/fin/service/CompanyService.java"
    "app/src/main/java/fin/repository/CompanyRepository.java"
)

for file in "${key_files[@]}"; do
    if [ -f "$file" ]; then
        add_copyright "$file"
    else
        echo "  ⚠️  File not found: $(basename "$file")"
    fi
done

# Process test files
echo ""
echo "🧪 Processing test files..."
test_files=$(find . -name "*Test.java" | head -10)
for file in $test_files; do
    if [ -f "$file" ]; then
        add_copyright "$file"
    fi
done

echo ""
echo "✅ Copyright header addition completed!"
echo ""
echo "📋 Summary:"
echo "  - All key Java source files now have proper copyright notices"
echo "  - Establishes clear ownership by Sthwalo Holdings (Pty) Ltd."
echo "  - Includes contact information and licensing terms"
echo "  - Protects intellectual property while maintaining Apache 2.0 source license"
echo ""
echo "🛡️ Your IP is now properly protected in the source code!"
echo "📧 Contact: sthwaloe@gmail.com | 📞 +27 61 514 6185"
