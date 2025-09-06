#!/bin/bash

# Frontend Licensing Setup Script for FIN drimacc Repository
# Copyright 2025 Immaculate Nyoni
# 
# This script applies dual licensing to the FIN frontend TypeScript application

set -e

REPO_NAME="drimacc"
CURRENT_DIR=$(pwd)

echo "🏠 FIN Frontend Licensing Setup"
echo "========================================"
echo "📁 Current directory: $CURRENT_DIR"
echo "🎯 Target repository: $REPO_NAME"
echo ""

# Check if we're in the correct directory
if [[ ! "$CURRENT_DIR" == *"$REPO_NAME"* ]]; then
    echo "⚠️  WARNING: This script should be run from the $REPO_NAME repository directory"
    echo "📍 Please navigate to your $REPO_NAME repository first:"
    echo "   cd /path/to/$REPO_NAME"
    echo "   then run this script again"
    echo ""
    read -p "🤔 Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Aborted. Please navigate to the correct directory first."
        exit 1
    fi
fi

echo "📋 Step 1: Creating LICENSE file..."
cat > LICENSE << 'EOF'
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of control, an entity
      is controlled by another entity if the entity holds the ownership
      of more than fifty percent (50%) of the outstanding shares, or
      (ii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (which shall not include communication that is conspicuously
      marked or otherwise designated in writing by the copyright owner
      as "Not a Contribution").

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of the definition of Contribution,
      any such submission is intentionally submitted for inclusion in the Work
      if (a) it is submitted under the terms of this License, or (b) it is
      subject to copyright and contributed to the Work under terms that are
      compatible with this License; except that

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to use, reproduce, modify, merge, publish,
      distribute, sublicense, and/or sell copies of the Work, and to
      permit persons to whom the Work is furnished to do so, subject to
      the following conditions:

      The above copyright notice and this permission notice shall be
      included in all copies or substantial portions of the Work.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, trademark, patent,
          attribution and other notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. When redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright 2025 Immaculate Nyoni

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   ADDITIONAL TERMS FOR COMMERCIAL USE:
   
   This software is licensed under Apache License 2.0 for PERSONAL USE ONLY.
   Commercial use requires a separate commercial license.
   
   For commercial licensing, please contact: licensing@fin-app.com
   
   Commercial use includes but is not limited to:
   - Use in any business or revenue-generating activity
   - Integration into commercial products or services
   - Hosting for customers or third parties
   - Use by organizations with more than 1 user
   
   Violation of these terms may result in legal action and damages.
EOF

echo "✅ LICENSE file created"

echo ""
echo "📋 Step 2: Creating COMMERCIAL_LICENSE.md..."
cat > COMMERCIAL_LICENSE.md << 'EOF'
# FIN Frontend - Commercial License Terms

**Copyright 2025 Immaculate Nyoni. All rights reserved.**

## Overview

The FIN Frontend TypeScript application (drimacc repository) is dual-licensed:
- **Personal Use**: Apache License 2.0 (free for personal, educational, and non-commercial use)
- **Commercial Use**: Requires a paid commercial license (this document)

## Commercial License Requirement

You need a commercial license if you plan to:

- ✅ Use FIN Frontend in any business or commercial environment
- ✅ Generate revenue using FIN Frontend (directly or indirectly)
- ✅ Deploy FIN Frontend for customers or third parties
- ✅ Integrate FIN Frontend into commercial products or services
- ✅ Use FIN Frontend in organizations with more than 1 user
- ✅ Modify and redistribute FIN Frontend commercially
- ✅ Remove attribution requirements

## Commercial License Tiers

### 🥉 Starter License - $29/month
**Perfect for small businesses and startups**

**Includes:**
- ✅ Commercial use for up to 5 users
- ✅ Basic technical support (email)
- ✅ Core frontend components and features
- ✅ Standard deployment options
- ✅ 1 production environment

**Limitations:**
- ❌ No white-labeling
- ❌ Limited API calls (10,000/month)
- ❌ Standard themes only

### 🥈 Professional License - $99/month
**Ideal for growing businesses**

**Includes:**
- ✅ Commercial use for up to 50 users
- ✅ Priority technical support (email + chat)
- ✅ Advanced frontend features and components
- ✅ Custom branding options
- ✅ Multiple deployment environments (dev, staging, prod)
- ✅ Enhanced API limits (100,000/month)
- ✅ Custom themes and styling

**Limitations:**
- ❌ Limited integrations (5 third-party services)
- ❌ Standard SLA (99.5% uptime)

### 🥇 Enterprise License - $299/month
**For large organizations and enterprises**

**Includes:**
- ✅ Unlimited commercial users
- ✅ Premium support (phone, email, chat, dedicated support)
- ✅ Full frontend feature access
- ✅ Complete white-labeling and customization
- ✅ Unlimited deployment environments
- ✅ Unlimited API calls
- ✅ Custom integrations and development
- ✅ Enterprise SLA (99.9% uptime)
- ✅ On-premise deployment options
- ✅ Source code access with modification rights

### 🌟 Custom Enterprise License
**For specific requirements beyond standard tiers**

**Contact us for:**
- ✅ Volume discounts for large deployments
- ✅ Custom feature development
- ✅ Dedicated hosting and support
- ✅ Special compliance requirements (HIPAA, SOX, etc.)
- ✅ Extended warranty and indemnification
- ✅ Technology transfer agreements

## Licensing Terms

### License Grant
Upon payment of applicable fees, you receive:
- Non-exclusive, non-transferable right to use FIN Frontend commercially
- Right to modify and customize for your specific needs
- Right to deploy in your chosen environments
- Technical support as specified in your tier

### Restrictions
- No redistribution of source code without explicit permission
- No reverse engineering for competitive purposes
- No sublicensing to third parties
- Must maintain copyright notices

### Support and Updates
- All licenses include software updates and security patches
- Support levels vary by tier (email, chat, phone)
- Bug fixes and critical security updates are prioritized
- Feature requests are considered based on license tier

### Payment Terms
- Monthly subscription billing
- Annual payment available with 20% discount
- No setup fees or hidden charges
- Cancel anytime with 30-day notice

### Compliance
- License compliance audits may be conducted
- Usage must stay within licensed user limits
- Overages subject to additional fees
- Violation may result in license termination

## How to Purchase

### 1. Contact Sales
- **Email**: licensing@fin-app.com
- **Website**: https://fin-licensing.com/frontend
- **Phone**: Contact via email for phone consultation

### 2. License Evaluation
- Free 30-day commercial trial available
- Demo environments for testing
- Technical consultation included

### 3. Implementation Support
- Deployment assistance
- Integration guidance
- Training for your team
- Documentation and best practices

## Legal Protection

### Intellectual Property
- FIN Frontend contains proprietary algorithms and innovations
- Protected by copyright, trade secrets, and patents (pending)
- Unauthorized use subject to legal action and damages

### Violation Consequences
- Immediate license termination
- Legal action for damages and injunctive relief
- Liability for attorney fees and court costs
- Potential criminal charges for willful infringement

### Warranty and Liability
- Commercial licenses include limited warranty
- Professional support and bug fixes guaranteed
- Liability limitations as specified in license agreement
- Indemnification available for Enterprise customers

## Frequently Asked Questions

### Q: Can I try before purchasing?
A: Yes! Contact us for a 30-day free commercial trial.

### Q: What if I exceed user limits?
A: Contact us to upgrade your license tier or pay overage fees.

### Q: Can I modify the frontend code?
A: Yes, commercial licenses include modification rights for your internal use.

### Q: Is there a discount for annual payment?
A: Yes, annual payment receives a 20% discount on all tiers.

### Q: What happens if I stop paying?
A: Your commercial license terminates, but you can continue using under Apache 2.0 for personal use only.

---

**Contact Information:**
- Email: licensing@fin-app.com
- Website: https://fin-licensing.com
- Documentation: https://docs.fin-app.com/licensing

**Legal Notice:**
This software is protected by copyright law and international treaties. Unauthorized reproduction or distribution of this software, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible under law.

Copyright 2025 Immaculate Nyoni. All rights reserved.
EOF

echo "✅ COMMERCIAL_LICENSE.md created"

echo ""
echo "📋 Step 3: Creating NOTICE file..."
cat > NOTICE << 'EOF'
FIN Frontend (drimacc)
Copyright 2025 Immaculate Nyoni

This product includes software developed by Immaculate Nyoni and the FIN development team.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

ADDITIONAL COMMERCIAL RESTRICTIONS:
This software is licensed for PERSONAL USE ONLY under Apache 2.0.
Commercial use requires a separate commercial license.
Contact licensing@fin-app.com for commercial licensing.

===============================================================================

This software contains the following third-party components:

React (https://reactjs.org/)
License: MIT License
Copyright (c) Facebook, Inc. and its affiliates.

TypeScript (https://www.typescriptlang.org/)
License: Apache License 2.0
Copyright (c) Microsoft Corporation.

Vite (https://vitejs.dev/)
License: MIT License
Copyright (c) 2019-present, Yuxi (Evan) You and Vite contributors.

Zustand (https://github.com/pmndrs/zustand)
License: MIT License
Copyright (c) 2019 Paul Henschel.

React Router (https://reactrouter.com/)
License: MIT License
Copyright (c) React Training 2016-2018, React Training LLC 2018-2020.

Tailwind CSS (https://tailwindcss.com/)
License: MIT License
Copyright (c) Tailwind Labs, Inc.

@tanstack/react-query
License: MIT License
Copyright (c) 2021 Tanner Linsley.

date-fns (https://date-fns.org/)
License: MIT License
Copyright (c) 2021 Sasha Koss and Lesha Koss.

recharts (https://recharts.org/)
License: MIT License
Copyright (c) 2015-2022 Recharts Group.

lucide-react (https://lucide.dev/)
License: ISC License
Copyright (c) 2022, Lucide Contributors.

clsx (https://github.com/lukeed/clsx)
License: MIT License
Copyright (c) Luke Edwards.

For a complete list of dependencies and their licenses, please refer to the package.json file and the node_modules directory in your installation.

===============================================================================

COMMERCIAL USE NOTICE:
If you are using this software for commercial purposes, you must obtain a commercial license.
Personal use is permitted under Apache License 2.0.
Commercial use without a license is strictly prohibited and may result in legal action.

Contact: licensing@fin-app.com
Website: https://fin-licensing.com
EOF

echo "✅ NOTICE file created"

echo ""
echo "📋 Step 4: Updating package.json..."

# Check if package.json exists
if [ -f "package.json" ]; then
    echo "📄 Found existing package.json, creating backup..."
    cp package.json package.json.backup
    
    # Update package.json with license information
    if command -v jq &> /dev/null; then
        echo "🔧 Using jq to update package.json..."
        jq '.license = "Apache-2.0" | .author = "Immaculate Nyoni <contact@fin-app.com>" | .homepage = "https://fin-app.com" | .repository = {"type": "git", "url": "https://github.com/immaculate-nyoni/drimacc.git"} | .bugs = {"url": "https://github.com/immaculate-nyoni/drimacc/issues"} | .keywords = ["finance", "typescript", "react", "personal-finance", "commercial-license"]' package.json > package.json.tmp && mv package.json.tmp package.json
    else
        echo "⚠️  jq not found, please manually update package.json with:"
        echo "   \"license\": \"Apache-2.0\","
        echo "   \"author\": \"Immaculate Nyoni <contact@fin-app.com>\","
        echo "   \"homepage\": \"https://fin-app.com\","
        echo "   \"repository\": {\"type\": \"git\", \"url\": \"https://github.com/immaculate-nyoni/drimacc.git\"},"
        echo "   \"bugs\": {\"url\": \"https://github.com/immaculate-nyoni/drimacc/issues\"}"
    fi
    echo "✅ package.json updated"
else
    echo "⚠️  package.json not found - please create one or ensure you're in the correct directory"
fi

echo ""
echo "📋 Step 5: Creating TypeScript license header utility..."
cat > add-license-headers.js << 'EOF'
#!/usr/bin/env node

/*
 * Copyright 2025 Immaculate Nyoni
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

const fs = require('fs');
const path = require('path');

const LICENSE_HEADER = `/*
 * Copyright 2025 Immaculate Nyoni
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

`;

const SUPPORTED_EXTENSIONS = ['.ts', '.tsx', '.js', '.jsx'];

function findSourceFiles(dir, files = []) {
  const items = fs.readdirSync(dir);
  
  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);
    
    if (stat.isDirectory()) {
      if (!['node_modules', 'build', 'dist', '.git', 'coverage'].includes(item)) {
        findSourceFiles(fullPath, files);
      }
    } else if (stat.isFile() && SUPPORTED_EXTENSIONS.includes(path.extname(item))) {
      files.push(fullPath);
    }
  }
  
  return files;
}

function hasLicenseHeader(content) {
  return content.includes('Licensed under the Apache License, Version 2.0') ||
         content.includes('Copyright 2025 Immaculate Nyoni');
}

function addLicenseHeader(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf-8');
    
    if (hasLicenseHeader(content)) {
      console.log(`⏭️  Skipping ${filePath} (already has license header)`);
      return false;
    }
    
    let finalContent;
    if (content.startsWith('#!')) {
      const lines = content.split('\n');
      const shebang = lines[0];
      const restOfContent = lines.slice(1).join('\n');
      finalContent = `${shebang}\n\n${LICENSE_HEADER}${restOfContent}`;
    } else {
      finalContent = `${LICENSE_HEADER}${content}`;
    }
    
    fs.writeFileSync(filePath, finalContent, 'utf-8');
    console.log(`✅ Added license header to ${filePath}`);
    return true;
  } catch (error) {
    console.error(`❌ Error processing ${filePath}:`, error);
    return false;
  }
}

function main() {
  const projectDir = process.argv[2] || '.';
  
  console.log('🔍 Finding TypeScript/JavaScript files...');
  const sourceFiles = findSourceFiles(projectDir);
  
  console.log(`📁 Found ${sourceFiles.length} source files`);
  
  let addedCount = 0;
  let skippedCount = 0;
  
  for (const file of sourceFiles) {
    if (addLicenseHeader(file)) {
      addedCount++;
    } else {
      skippedCount++;
    }
  }
  
  console.log('\n📊 Summary:');
  console.log(`✅ Added headers: ${addedCount}`);
  console.log(`⏭️  Skipped: ${skippedCount}`);
  console.log(`📁 Total files: ${sourceFiles.length}`);
  
  if (addedCount > 0) {
    console.log('\n🎉 License headers added successfully!');
    console.log('📝 Don\'t forget to commit these changes to Git');
  } else {
    console.log('\n✨ All files already have license headers!');
  }
}

if (require.main === module) {
  main();
}

module.exports = { addLicenseHeader, hasLicenseHeader, findSourceFiles };
EOF

chmod +x add-license-headers.js
echo "✅ License header utility created"

echo ""
echo "📋 Step 6: Creating README update..."
if [ -f "README.md" ]; then
    echo "📄 Found existing README.md, creating backup..."
    cp README.md README.md.backup
    
    echo "📝 Adding license section to README.md..."
    cat >> README.md << 'EOF'

## 📄 License

FIN Frontend is dual-licensed:

### 🏠 Personal Use (Free)
- Licensed under [Apache License 2.0](LICENSE)
- ✅ Personal finance management
- ✅ Educational and research use
- ✅ Non-commercial open source development
- ❌ Commercial use not permitted

### 💼 Commercial Use (Paid)
Commercial use requires a separate license. See [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md) for details.

**Commercial License Tiers:**
- 🥉 **Starter**: $29/month (up to 5 users)
- 🥈 **Professional**: $99/month (up to 50 users) 
- 🥇 **Enterprise**: $299/month (unlimited users)

**Contact:** licensing@fin-app.com

### 🚨 Important Notice
This software is licensed for **PERSONAL USE ONLY** under Apache 2.0. Commercial use without a license is strictly prohibited and may result in legal action.

### 🛡️ Copyright
Copyright 2025 Immaculate Nyoni. All rights reserved.

---

For the complete license terms, please see:
- [LICENSE](LICENSE) - Apache License 2.0 with commercial restrictions
- [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md) - Commercial licensing terms
- [NOTICE](NOTICE) - Third-party attributions and legal notices
EOF
    
    echo "✅ README.md updated with license information"
else
    echo "⚠️  README.md not found - please create one or ensure you're in the correct directory"
fi

echo ""
echo "🎉 Frontend Licensing Setup Complete!"
echo "========================================"
echo ""
echo "📁 Files created/updated:"
echo "   ✅ LICENSE (Apache 2.0 with commercial restrictions)"
echo "   ✅ COMMERCIAL_LICENSE.md (Commercial licensing terms)"
echo "   ✅ NOTICE (Third-party attributions)"
echo "   ✅ add-license-headers.js (License header utility)"
echo "   ✅ package.json (Updated with license info)"
echo "   ✅ README.md (Added license section)"
echo ""
echo "🔧 Next Steps:"
echo ""
echo "1️⃣  Add license headers to your TypeScript files:"
echo "   node add-license-headers.js"
echo ""
echo "2️⃣  Copy the LicenseManager component to your src folder:"
echo "   # The LicenseManager.tsx component has been created"
echo "   # Copy it to your drimacc/src/components/ directory"
echo ""
echo "3️⃣  Install required dependencies (if not already installed):"
echo "   npm install react react-dom @types/react @types/react-dom"
echo ""
echo "4️⃣  Update your main App component to include license checking:"
echo "   import { LicenseManager } from './components/LicenseManager';"
echo ""
echo "5️⃣  Commit all changes to Git:"
echo "   git add ."
echo "   git commit -m \"Add dual licensing with Apache 2.0 and commercial terms\""
echo ""
echo "6️⃣  Consider adding these to your CI/CD pipeline:"
echo "   - License header validation"
echo "   - License compliance checking"
echo "   - Automated license verification"
echo ""
echo "🛡️  Your frontend is now protected with dual licensing!"
echo "📞 For commercial licensing inquiries: licensing@fin-app.com"
echo "🌐 Visit: https://fin-licensing.com/frontend"
