#!/bin/bash

# Script to apply dual licensing to your drimacc frontend repository
# Run this script from your drimacc root directory

echo "🛡️ Applying FIN Dual Licensing to TypeScript Frontend (drimacc)"
echo "================================================================="

# Create LICENSE file (Apache 2.0 with commercial restriction)
echo "📄 Creating LICENSE file..."
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
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (which shall not include communication that is conspicuously
      marked or otherwise designated in writing by the copyright owner
      as "Not a Contribution.")

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based upon (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and derivative works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control
      systems, and issue tracking systems that are managed by, or on behalf
      of, the Licensor for the purpose of discussing and improving the Work,
      but excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to use, reproduce, modify, display, perform,
      sublicense, and distribute the Work and such Derivative Works in
      Source or Object form.

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

      (d) If the Work includes a "NOTICE" file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE file distributed
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

      You may add Your own copyright notice on Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

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

   9. Accepting Warranty or Support. You may choose to offer, and to
      charge a fee for, warranty, support, indemnity or other liability
      obligations and/or rights consistent with this License. However, in
      accepting such obligations, You may act only on Your own behalf and on
      Your sole responsibility, not on behalf of any other Contributor, and
      only if You agree to indemnify, defend, and hold each Contributor
      harmless for any liability incurred by, or claims asserted against,
      such Contributor by reason of your accepting any such warranty or support.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      IMPORTANT: This Apache License 2.0 applies ONLY to the source code
      for development, modification, and personal use. Commercial use of
      the APPLICATION requires a separate commercial license.
      
      See COMMERCIAL_LICENSE.md for commercial terms and subscription options.

      Copyright 2024 Immaculate Nyoni

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.
EOF

echo "✅ LICENSE file created"

# Create COMMERCIAL_LICENSE.md
echo "📄 Creating COMMERCIAL_LICENSE.md..."
cat > COMMERCIAL_LICENSE.md << 'EOF'
# FIN Frontend Financial Interface - Commercial License Terms

**Copyright 2025 Immaculate Nyoni. All Rights Reserved.**

## 📋 **License Summary**

| Use Case | License Required | Terms |
|----------|------------------|-------|
| **Personal Use** | Apache 2.0 (Free) | Individual users, personal finance management |
| **Educational Use** | Apache 2.0 (Free) | Students, researchers, educational institutions |
| **Open Source Development** | Apache 2.0 (Free) | Contributing to FIN project, non-commercial development |
| **Commercial Use** | Commercial License (Paid) | Business use, revenue generation, commercial deployment |

## 🚫 **Important: Commercial Use Restrictions**

**The Apache 2.0 license does NOT grant commercial use rights for the FRONTEND APPLICATION.**

Commercial use includes but is not limited to:
- Using FIN frontend to manage business finances
- Providing financial services using FIN interface
- Hosting FIN frontend for paying customers
- Integrating FIN frontend into commercial products
- Using FIN frontend in any revenue-generating activity
- White-labeling or rebranding for commercial purposes

## 💼 **Commercial License Options**

### **🥉 Starter License - $29/month**
**Perfect for small businesses and startups**

**Frontend Limitations:**
- Up to 3 company profiles
- Basic dashboard and reporting
- Standard templates and themes
- Email support only
- Single user interface

**Includes:**
- ✅ Full frontend application access
- ✅ Document upload and OCR processing
- ✅ Basic financial reports and charts
- ✅ CSV/Excel/PDF export functionality
- ✅ Email support (48h response)

### **🥈 Professional License - $99/month**
**For growing businesses with complex needs**

**Frontend Limitations:**
- Up to 10 company profiles
- Advanced dashboard customization
- Premium templates and themes
- Priority support
- Up to 5 user accounts

**Includes:**
- ✅ Everything in Starter
- ✅ Advanced reporting suite with custom charts
- ✅ Multi-company dashboard views
- ✅ Custom branding options
- ✅ Advanced export formats
- ✅ Priority support (24h response)
- ✅ Custom dashboard layouts

### **🥇 Enterprise License - $299/month**
**For large organizations and financial firms**

**No Frontend Limitations:**
- Unlimited company profiles
- Full customization capabilities
- White-label branding rights
- Unlimited user accounts
- Phone and email support

**Includes:**
- ✅ Everything in Professional
- ✅ Complete white-label customization
- ✅ Advanced compliance dashboards
- ✅ Real-time collaboration features
- ✅ Priority phone support (4h response)
- ✅ Custom component development
- ✅ API integration assistance
- ✅ Training and onboarding

### **🏢 Custom License - Contact for Pricing**
**For specialized industries and unique requirements**

- Industry-specific compliance interfaces
- Custom component development
- Multi-tenant SaaS deployment
- Source code escrow
- Dedicated UI/UX team
- Training and certification

## 🛡️ **Intellectual Property Protection**

### **Frontend Design Protection**
This frontend interface, its unique design patterns, user experience workflows, and visual implementations are protected by copyright and trade secrets. Any attempt to copy, reverse engineer, or create similar interfaces for commercial purposes without proper licensing is strictly prohibited.

### **Component Library Protection**
- All React components: Copyright 2025 Immaculate Nyoni
- All TypeScript interfaces and types: Proprietary
- All styling and design systems: Proprietary
- All UI/UX patterns and workflows: Trade secrets

### **Technology Stack Protection**
- Custom Zustand store implementations: Proprietary
- OCR and ML integration patterns: Trade secrets
- Export and reporting algorithms: Proprietary
- Performance optimization techniques: Trade secrets

## ⚖️ **Legal Enforcement**

### **Violation Consequences**
Unauthorized commercial use will result in:
1. **Immediate cease and desist notice**
2. **Retroactive licensing fees** (3x standard rate)
3. **Legal action** for damages and injunctive relief
4. **Public disclosure** of violation
5. **Interface access termination**

### **Monitoring and Detection**
We actively monitor for unauthorized commercial use through:
- Automated deployment scanning
- Business technology stack monitoring
- User reports and community surveillance
- Legal technology tracking services
- Domain and hosting analysis

## 📞 **Getting Licensed**

### **Start Your Commercial License:**
1. **Visit:** https://fin-licensing.com/frontend
2. **Email:** frontend-licensing@fin-software.com
3. **Phone:** +1-xxx-xxx-xxxx
4. **Schedule:** Free frontend demo and consultation

### **License Activation Process:**
1. Choose your frontend license tier
2. Complete business verification
3. Sign frontend license agreement
4. Receive activation keys and access
5. Download commercial frontend build

### **Payment Terms:**
- Monthly or annual billing
- 30-day free trial for all tiers
- No setup fees
- Cancel anytime (with 30-day notice)

## 🎯 **Why License FIN Frontend Commercially?**

### **For Your Business:**
- ✅ **Legal Protection** - Fully compliant commercial use
- ✅ **Professional Support** - UI/UX help when you need it
- ✅ **Regular Updates** - Latest features and security patches
- ✅ **Custom Branding** - Make it look like your application
- ✅ **Scalability** - Grow without limitations

### **For the Community:**
- ✅ **Continued Development** - Your licensing supports ongoing innovation
- ✅ **Free Personal Use** - Keeps the personal version free forever
- ✅ **Open Source Core** - Maintains transparency and security
- ✅ **Industry Advancement** - Pushes frontend financial technology forward

## 🚨 **Frequently Asked Questions**

### **Q: Can I use the frontend components in my business app?**
**A:** No. Having access to source code under Apache 2.0 does not grant commercial use rights for the frontend application. You need a commercial license.

### **Q: What if I modify the frontend code?**
**A:** Modifications for personal use are allowed under Apache 2.0. Commercial use of any derivative frontend work requires our commercial license.

### **Q: Can I host the frontend for my clients?**
**A:** Only with an appropriate commercial license. Hosting the frontend for others is considered commercial use.

### **Q: What about non-profit organizations?**
**A:** Non-profits using the frontend for their internal operations (not providing services to others) qualify for educational use under Apache 2.0.

### **Q: Can I remove the FIN branding?**
**A:** Only with Enterprise or Custom licenses that include white-label rights.

---

**© 2025 Immaculate Nyoni. All rights reserved.**

**This commercial license is required for any business or commercial use of FIN Frontend Financial Interface. Unauthorized commercial use is strictly prohibited and subject to legal action.**

**Contact frontend-licensing@fin-software.com for questions or to begin your commercial frontend license.**
EOF

echo "✅ COMMERCIAL_LICENSE.md created"

# Create NOTICE file
echo "📄 Creating NOTICE..."
cat > NOTICE << 'EOF'
FIN Frontend Financial Interface
Copyright 2025 Immaculate Nyoni

This product includes software developed at
GitHub (https://github.com/sthwalo/drimacc)

This frontend software contains the following third-party components:

1. React
   Copyright Facebook, Inc. and its affiliates
   Licensed under the MIT License

2. TypeScript
   Copyright Microsoft Corporation
   Licensed under the Apache License, Version 2.0

3. Vite
   Copyright 2019-present, Yuxi (Evan) You and Vite contributors
   Licensed under the MIT License

4. Tailwind CSS
   Copyright Tailwind Labs, Inc.
   Licensed under the MIT License

5. Zustand
   Copyright 2019 Paul Henschel
   Licensed under the MIT License

6. Tesseract.js
   Copyright 2017 Guillermo Webster
   Licensed under the Apache License, Version 2.0

7. TensorFlow.js
   Copyright 2018 Google Inc.
   Licensed under the Apache License, Version 2.0

8. ExcelJS
   Copyright Guyon Roche and contributors
   Licensed under the MIT License

9. jsPDF
   Copyright James Hall and contributors
   Licensed under the MIT License

10. Recharts
    Copyright Recharts Group
    Licensed under the MIT License

For complete license texts, see the respective license files or visit:
- Apache License 2.0: http://www.apache.org/licenses/LICENSE-2.0
- MIT License: https://opensource.org/licenses/MIT
EOF

echo "✅ NOTICE file created"

echo ""
echo "🎉 FIN Frontend Dual Licensing Setup Complete!"
echo "=============================================="
echo ""
echo "📁 Files Created:"
echo "   ✅ LICENSE (Apache 2.0 with commercial restrictions)"
echo "   ✅ COMMERCIAL_LICENSE.md (Commercial terms and pricing)" 
echo "   ✅ NOTICE (Third-party attributions)"
echo ""
echo "🔧 Next Steps:"
echo "   1. Update your README.md with licensing information"
echo "   2. Add license headers to main TypeScript files"
echo "   3. Update package.json with license information"
echo "   4. Create license verification component"
echo "   5. Commit and push to your drimacc repository"
echo ""
echo "🛡️ Your frontend is now protected with dual licensing!"
EOF

chmod +x frontend-licensing-setup.sh
