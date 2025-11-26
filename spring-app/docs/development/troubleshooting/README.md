# Troubleshooting Documentation Index

## Overview
This directory contains troubleshooting documentation for known issues in the FIN Financial Management System. Each issue is documented with symptoms, root cause analysis, debugging steps, and resolution approaches.

## Current Issues

### Active Issues

#### 1. Fiscal Periods - Empty Array Error on New Companies
**File**: `fiscal-periods-empty-array-error.md`
**Status**: Active - Documentation created, solution brainstorming needed
**Date Reported**: 26 November 2025
**Priority**: High - Blocks new company onboarding
**Summary**: Frontend throws error instead of loading create fiscal period view when new company has no fiscal periods

### Resolved Issues

#### 1. 403 Forbidden on /api/v1/companies/user Endpoint
**File**: `403-forbidden-companies-endpoint.md`
**Status**: Active - Debugging in progress
**Date Reported**: 18 November 2025
**Priority**: High - Blocks user access to companies
**Summary**: Users receive 403 Forbidden on companies endpoint despite valid authentication

## Issue Status Legend
- **Active**: Issue confirmed, debugging/documentation in progress
- **Resolved**: Issue fixed, documentation updated with solution
- **Closed**: Issue resolved and verified, no further action needed
- **Archived**: Issue resolved, documentation kept for reference

## Contributing to Troubleshooting
When documenting a new issue:

1. **Create Issue File**: Use descriptive filename with issue summary
2. **Follow Template**: Include all sections (Summary, Symptoms, Root Cause, etc.)
3. **Update Index**: Add entry to this README with status and priority
4. **Status Updates**: Update status as issue progresses through resolution

## Common Issue Categories
- **Authentication/Authorization**: Login, permissions, JWT issues
- **API Integration**: Frontend-backend communication problems
- **Database Issues**: Data integrity, query failures, migrations
- **UI/UX Problems**: Frontend rendering, state management, user flows
- **Business Logic**: Incorrect calculations, validation failures, workflow issues

## Quick Reference
- **Most Recent Issue**: Fiscal periods empty array error (26 Nov 2025)
- **Highest Priority**: Fiscal periods error (blocks new company setup)
- **Total Active Issues**: 2
- **Total Resolved Issues**: 0</content>
<parameter name="filePath">/Users/sthwalonyoni/FIN/docs/troubleshooting/README.md