#!/bin/bash

# 🚀 GitHub Actions Pipeline Deployment Script
# This script commits and pushes all CI/CD pipeline files to activate GitHub Actions

echo "🎯 FIN Project - GitHub Actions Pipeline Deployment"
echo "=================================================="

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    echo "❌ Error: Not in a git repository. Please run 'git init' first."
    exit 1
fi

# Check git status
echo "📊 Current Git Status:"
git status --short

echo ""
echo "📁 Pipeline Files to Commit:"
echo "  - .github/workflows/ci-cd.yml (Main CI/CD Pipeline)"
echo "  - .github/workflows/pr-validation.yml (PR Validation)"  
echo "  - .github/workflows/release.yml (Release & Deploy)"

echo ""
read -p "🤔 Do you want to commit and push these pipeline files? (y/N): " confirm

if [[ ! $confirm =~ ^[Yy]$ ]]; then
    echo "❌ Cancelled by user."
    exit 0
fi

# Add the workflow files
echo ""
echo "📝 Adding workflow files to git..."
git add .github/workflows/

# Check if there are other changes to include
if git diff --cached --quiet && git diff --quiet; then
    echo "⚠️  No changes to commit."
else
    # Show what will be committed
    echo ""
    echo "📋 Files being committed:"
    git diff --cached --name-only

    echo ""
    echo "🏗️ Committing GitHub Actions pipelines..."
    git commit -m "🚀 Add comprehensive GitHub Actions CI/CD pipeline

✨ Features:
- Complete CI/CD pipeline with 7 jobs
- PR validation workflow with auto-comments
- Release management and deployment
- Comprehensive test matrix (UI, Controllers, Services, State)
- Performance monitoring and quality checks
- Artifact management with retention policies

🔧 Technical Details:
- Java 17 with Oracle distribution
- Gradle build system with memory optimization
- H2 in-memory database for testing
- JUnit Jupiter 5 + Mockito 5.5.0
- Matrix testing strategy for parallel execution
- Automated release notes generation

🎯 Workflow Coverage:
- ci-cd.yml: Main pipeline (build/test/quality/performance/deploy)
- pr-validation.yml: Quick PR validation with GitHub comments  
- release.yml: Release management and deployment automation

Ready for GitHub Actions activity monitoring! 🎉"

    # Check if we have a remote
    if git remote | grep -q origin; then
        echo ""
        echo "🚀 Pushing to GitHub..."
        
        # Get current branch
        current_branch=$(git branch --show-current)
        echo "📍 Current branch: $current_branch"
        
        # Push to origin
        if git push origin "$current_branch"; then
            echo ""
            echo "🎉 SUCCESS! GitHub Actions pipelines have been deployed!"
            echo ""
            echo "🔗 Next Steps:"
            echo "   1. Visit: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[\/:]//; s/.git$//')/actions"
            echo "   2. Watch the 'Build & Test Pipeline' workflow run automatically"
            echo "   3. Create a Pull Request to see the PR validation in action"
            echo "   4. Create a release to trigger the release pipeline"
            echo ""
            echo "📊 Available Workflows:"
            echo "   • 🔄 Build & Test Pipeline (scheduled + push)"
            echo "   • 🚦 Pull Request Validation (PR events)" 
            echo "   • 🚀 Release & Deploy (releases + manual)"
            echo ""
            echo "🎯 GitHub Activity Dashboard will now show comprehensive automation!"
        else
            echo "❌ Error: Failed to push to GitHub."
            echo "💡 Try: git push --set-upstream origin $current_branch"
            exit 1
        fi
    else
        echo ""
        echo "⚠️  No remote 'origin' configured."
        echo "💡 Add your GitHub repository:"
        echo "   git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPO.git"
        echo "   git push -u origin main"
    fi
fi

echo ""
echo "✅ Pipeline deployment complete!"
echo "🔍 Monitor activity at: https://github.com/YOUR-USERNAME/YOUR-REPO/actions"
