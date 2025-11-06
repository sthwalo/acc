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

package fin.controller;

import fin.model.Asset;
import fin.model.DepreciationMethod;
import fin.model.DepreciationRequest;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;
import fin.service.DepreciationService;
import fin.state.ApplicationState;
import fin.ui.InputHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller for depreciation calculator functionality
 */
public class DepreciationController {
    // Magic number constants
    private static final int TABLE_WIDTH = 80;
    private static final int COMPARISON_TABLE_WIDTH = 100;
    private static final int SCHEDULE_TABLE_WIDTH = 70;
    private static final int FIN_RECOVERY_PERIOD_5 = 5;
    private static final int FIN_RECOVERY_PERIOD_7 = 7;
    private static final int MENU_OPTION_1 = 1;
    private static final int MENU_OPTION_2 = 2;
    private static final int MENU_OPTION_3 = 3;
    private static final int MENU_OPTION_4 = 4;
    private static final int MENU_OPTION_5 = 5;
    private static final int MENU_OPTION_6 = 6;
    private static final BigDecimal ZERO_SALVAGE = BigDecimal.ZERO;

    private final DepreciationService depreciationService;
    private final ApplicationState applicationState;
    private final InputHandler inputHandler;

    public DepreciationController(DepreciationService depreciationServiceParam,
                                ApplicationState applicationStateParam,
                                InputHandler inputHandlerParam) {
        this.depreciationService = depreciationServiceParam;
        this.applicationState = applicationStateParam;
        this.inputHandler = inputHandlerParam;
    }

    /**
     * Main depreciation calculator menu
     */
    public void displayDepreciationMenu() {
        while (true) {
            System.out.println("\n===== Depreciation Calculator =====");
            System.out.println("1. Calculate and Save Depreciation Schedule");
            System.out.println("2. View Saved Depreciation Schedules");
            System.out.println("3. Manage Assets");
            System.out.println("4. Quick Calculation (In-Memory)");
            System.out.println("5. Repost Depreciation Schedules");
            System.out.println("6. Back to Main Menu");
            System.out.print("Enter your choice (1-6): ");

            int choice = inputHandler.getInteger("Enter your choice");

            switch (choice) {
                case MENU_OPTION_1:
                    calculateAndSaveDepreciation();
                    break;
                case MENU_OPTION_2:
                    viewSavedSchedules();
                    break;
                case MENU_OPTION_3:
                    manageAssets();
                    break;
                case MENU_OPTION_4:
                    displayQuickCalculationMenu();
                    break;
                case MENU_OPTION_5:
                    repostDepreciationSchedules();
                    break;
                case MENU_OPTION_6:
                    return;
                default:
                    System.out.println("❌ Invalid choice. Please try again.");
            }
        }
    }

    private void calculateAndSaveDepreciation() {
        System.out.println("\n=== Calculate and Save Depreciation Schedule ===");

        try {
            // First, get asset code and try to find existing asset
            String assetCode = inputHandler.getString("Enter asset code");
            Long companyId = applicationState.getCurrentCompany().getId();

            // Try to find existing asset by code
            Optional<Asset> existingAssetOpt = depreciationService.getAssetByCode(assetCode, companyId);
            Asset asset;

            if (existingAssetOpt.isPresent()) {
                // Use existing asset
                asset = existingAssetOpt.get();
                System.out.printf("✅ Found existing asset: %s (%s)%n", asset.getAssetName(), asset.getAssetCode());
                System.out.printf("Cost: R%.2f, Useful Life: %d years, Salvage Value: R%.2f%n",
                    asset.getCost(), asset.getUsefulLifeYears(), asset.getSalvageValue());
            } else {
                // Asset doesn't exist, ask user if they want to create it
                System.out.printf("⚠️  Asset with code '%s' not found.%n", assetCode);
                String createAsset = inputHandler.getString("Create new asset? (Y/n)");
                if (!createAsset.isEmpty() && !createAsset.toLowerCase().startsWith("y")) {
                    System.out.println("❌ Operation cancelled.");
                    return;
                }

                // Get remaining asset information
                String assetName = inputHandler.getString("Enter asset name");
                String assetDescription = inputHandler.getString("Enter asset description");
                BigDecimal cost = inputHandler.getBigDecimal("Enter asset cost");
                LocalDate purchaseDate = inputHandler.getDate("Enter purchase date");
                int usefulLife = inputHandler.getInteger("Enter useful life (years)");
                BigDecimal salvageValue = inputHandler.getBigDecimal("Enter salvage value");

                // Create new asset
                asset = new Asset();
                asset.setCompanyId(companyId);
                asset.setAssetCode(assetCode);
                asset.setAssetName(assetName);
                asset.setDescription(assetDescription);
                asset.setCost(cost);
                asset.setAcquisitionDate(purchaseDate);
                asset.setUsefulLifeYears(usefulLife);
                asset.setSalvageValue(salvageValue);
                asset.setAssetCategory("FIXED_ASSET");
                asset.setLocation("MAIN_OFFICE");
                asset.setDepartment("GENERAL");
                asset.setCreatedBy("FIN");

                // Save asset
                asset = depreciationService.saveAsset(asset);
                System.out.println("✅ Asset created successfully with ID: " + asset.getId());
            }

            // Get depreciation method
            DepreciationMethod method = selectDepreciationMethod();

            BigDecimal dbFactor = null;
            if (method == DepreciationMethod.DECLINING_BALANCE) {
                // Display rate options
                System.out.println("\nSelect declining balance rate:");
                System.out.println("1. 20%");
                System.out.println("2. 25%");
                System.out.println("3. 30%");
                System.out.println("4. 33.33%");
                System.out.println("5. 35%");

                int choice = inputHandler.getInteger("Enter your choice (1-5)");
                BigDecimal selectedRate;
                switch (choice) {
                    case 1: selectedRate = BigDecimal.valueOf(20); break;
                    case 2: selectedRate = BigDecimal.valueOf(25); break;
                    case 3: selectedRate = BigDecimal.valueOf(30); break;
                    case 4: selectedRate = BigDecimal.valueOf(33.33); break;
                    case 5: selectedRate = BigDecimal.valueOf(35); break;
                    default:
                        System.out.println("❌ Invalid choice. Using 20% as default.");
                        selectedRate = BigDecimal.valueOf(20);
                }

                // Calculate factor: factor = rate / 100 (annual percentage rate)
                dbFactor = selectedRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

                System.out.printf("Selected rate: %.2f%%, Calculated factor: %.2f%n", selectedRate, dbFactor);
            }

            // Create depreciation request using asset data
            DepreciationRequest request = new DepreciationRequest();
            request.setCost(asset.getCost());
            request.setSalvageValue(asset.getSalvageValue());
            request.setUsefulLife(asset.getUsefulLifeYears());
            request.setMethod(method);
            request.setDbFactor(dbFactor);

            // Calculate and save depreciation
            DepreciationSchedule schedule = depreciationService.calculateAndSaveDepreciation(asset, request);

            System.out.println("✅ Depreciation schedule created successfully!");
            System.out.println("Asset: " + asset.getAssetName() + " (" + asset.getAssetCode() + ")");
            System.out.println("Method: " + method.name());
            System.out.println("Total depreciation: " + schedule.getTotalDepreciation());

        } catch (InputHandler.InputCancelledException e) {
            System.out.println("❌ Operation cancelled. Returning to menu.");
        } catch (Exception e) {
            System.out.println("❌ Error calculating and saving depreciation: " + e.getMessage());
        }
    }

    private void viewSavedSchedules() {
        System.out.println("\n=== Saved Depreciation Schedules ===");

        try {
            List<Asset> assets = depreciationService.getAssetsForCompany(applicationState.getCurrentCompany().getId());

            if (assets.isEmpty()) {
                System.out.println("📭 No assets found for the current company.");
                return;
            }

            System.out.println("Available Assets:");
            System.out.printf("%-5s %-20s %-15s %-15s %-10s %-15s %-10s%n",
                "ID", "Name", "Cost", "Purchase Date", "Useful Life", "Salvage Value", "Has Schedule");
            System.out.println("=".repeat(TABLE_WIDTH));

            for (Asset asset : assets) {
                String hasSchedule = depreciationService.getDepreciationScheduleForAsset(asset.getId()).isPresent()
                    ? "Yes" : "No";
                System.out.printf("%-5d %-20s %-15s %-15s %-10d %-15s %-10s%n",
                    asset.getId(), asset.getAssetName(),
                    asset.getCost(), asset.getAcquisitionDate(),
                    asset.getUsefulLifeYears(), asset.getSalvageValue(), hasSchedule);
            }

            System.out.print("\nEnter asset ID to view depreciation schedule (0 to cancel): ");
            Long assetId = (long) inputHandler.getInteger("Enter asset ID");

            if (assetId == 0) {
                return;
            }

            var scheduleOpt = depreciationService.getDepreciationScheduleForAsset(assetId);
            if (scheduleOpt.isPresent()) {
                var assetOpt = depreciationService.getAssetById(assetId);
                if (assetOpt.isPresent()) {
                    displayDepreciationSchedule(scheduleOpt.get(),
                        scheduleOpt.get().getDepreciationMethod() + " Depreciation for " + assetOpt.get().getAssetName());
                }
            } else {
                System.out.println("❌ No depreciation schedule found for asset ID: " + assetId);
            }

        } catch (Exception e) {
            System.out.println("❌ Error viewing saved schedules: " + e.getMessage());
        }
    }

    private void manageAssets() {
        while (true) {
            System.out.println("\n=== Asset Management ===");
            System.out.println("1. List All Assets");
            System.out.println("2. Add New Asset");
            System.out.println("3. Update Asset");
            System.out.println("4. Delete Asset");
            System.out.println("5. Back to Depreciation Menu");
            System.out.print("Enter your choice (1-5): ");

            int choice = inputHandler.getInteger("Enter your choice");

            switch (choice) {
                case 1:
                    listAssets();
                    break;
                case 2:
                    addAsset();
                    break;
                case 3:
                    updateAsset();
                    break;
                case 4:
                    deleteAsset();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("❌ Invalid choice. Please try again.");
            }
        }
    }

    private void listAssets() {
        try {
            List<Asset> assets = depreciationService.getAssetsForCompany(applicationState.getCurrentCompany().getId());

            if (assets.isEmpty()) {
                System.out.println("📭 No assets found for the current company.");
                return;
            }

            System.out.println("\nAssets for Current Company:");
            System.out.printf("%-5s %-20s %-15s %-15s %-15s %-12s %-15s%n",
                "ID", "Name", "Description", "Cost", "Purchase Date", "Useful Life", "Salvage Value");
            System.out.println("=".repeat(TABLE_WIDTH));

            for (Asset asset : assets) {
                System.out.printf("%-5d %-20s %-15s %-15s %-15s %-12d %-15s%n",
                    asset.getId(), asset.getAssetName(),
                    asset.getDescription(), asset.getCost(), asset.getAcquisitionDate(),
                    asset.getUsefulLifeYears(), asset.getSalvageValue());
            }

        } catch (Exception e) {
            System.out.println("❌ Error listing assets: " + e.getMessage());
        }
    }

    private void addAsset() {
        try {
            System.out.println("\n=== Add New Asset ===");

            String assetCode = inputHandler.getString("Enter asset code");
            Long companyId = applicationState.getCurrentCompany().getId();

            // Check if asset code already exists
            if (depreciationService.assetCodeExists(assetCode, companyId)) {
                System.out.printf("⚠️  Asset code '%s' already exists for this company.%n", assetCode);
                String generateUnique = inputHandler.getString("Generate unique code automatically? (Y/n)");
                if (generateUnique.isEmpty() || generateUnique.toLowerCase().startsWith("y")) {
                    assetCode = depreciationService.generateUniqueAssetCode(assetCode, companyId);
                    System.out.printf("✅ Generated unique code: '%s'%n", assetCode);
                } else {
                    System.out.println("❌ Asset creation cancelled.");
                    return;
                }
            }

            String name = inputHandler.getString("Enter asset name");
            String description = inputHandler.getString("Enter asset description");
            BigDecimal cost = inputHandler.getBigDecimal("Enter asset cost");
            LocalDate purchaseDate = inputHandler.getDate("Enter purchase date (YYYY-MM-DD)");
            int usefulLife = inputHandler.getInteger("Enter useful life (years)");
            BigDecimal salvageValue = inputHandler.getBigDecimal("Enter salvage value");

            Asset asset = new Asset();
            asset.setCompanyId(companyId);
            asset.setAssetCode(assetCode);
            asset.setAssetName(name);
            asset.setDescription(description);
            asset.setCost(cost);
            asset.setAcquisitionDate(purchaseDate);
            asset.setUsefulLifeYears(usefulLife);
            asset.setSalvageValue(salvageValue);

            Asset savedAsset = depreciationService.saveAsset(asset);
            System.out.println("✅ Asset saved successfully with ID: " + savedAsset.getId());

        } catch (InputHandler.InputCancelledException e) {
            System.out.println("❌ Operation cancelled. Returning to menu.");
        } catch (Exception e) {
            System.out.println("❌ Error adding asset: " + e.getMessage());
        }
    }

    private void updateAsset() {
        try {
            listAssets();
            System.out.print("\nEnter asset ID to update (0 to cancel): ");
            Long assetId = (long) inputHandler.getInteger("Enter asset ID");

            if (assetId == 0) {
                return;
            }

            var assetOpt = depreciationService.getAssetById(assetId);
            if (assetOpt.isEmpty()) {
                System.out.println("❌ Asset not found.");
                return;
            }

            Asset asset = assetOpt.get();
            System.out.println("Current values:");
            System.out.printf("Name: %s%n", asset.getAssetName());
            System.out.printf("Description: %s%n", asset.getDescription());
            System.out.printf("Cost: R%.2f%n", asset.getCost());
            System.out.printf("Purchase Date: %s%n", asset.getAcquisitionDate());
            System.out.printf("Useful Life: %d years%n", asset.getUsefulLifeYears());
            System.out.printf("Salvage Value: R%.2f%n", asset.getSalvageValue());

            System.out.println("\nEnter new values (press Enter to keep current value):");

            // Get asset information
            String name = inputHandler.getString("Enter new asset name (leave empty to keep current)");
            String description = inputHandler.getString("Enter new description (leave empty to keep current)");
            String costStr = inputHandler.getString("Enter new cost (leave empty to keep current)");
            String dateStr = inputHandler.getString("Enter new purchase date (YYYY-MM-DD, leave empty to keep current)");
            String lifeStr = inputHandler.getString("Enter new useful life (years, leave empty to keep current)");
            String salvageStr = inputHandler.getString("Enter new salvage value (leave empty to keep current)");

            // Update asset fields
            if (!name.isEmpty()) asset.setAssetName(name);
            if (!description.isEmpty()) asset.setDescription(description);
            if (!costStr.isEmpty()) asset.setCost(new BigDecimal(costStr));
            if (!dateStr.isEmpty()) asset.setAcquisitionDate(LocalDate.parse(dateStr));
            if (!lifeStr.isEmpty()) asset.setUsefulLifeYears(Integer.parseInt(lifeStr));
            if (!salvageStr.isEmpty()) asset.setSalvageValue(new BigDecimal(salvageStr));

            // Save updated asset
            depreciationService.saveAsset(asset);

            System.out.println("✅ Asset updated successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error updating asset: " + e.getMessage());
        }
    }

    private void deleteAsset() {
        try {
            listAssets();
            System.out.print("\nEnter asset ID to delete (0 to cancel): ");
            Long assetId = (long) inputHandler.getInteger("Enter asset ID");

            if (assetId == 0) {
                return;
            }

            var assetOpt = depreciationService.getAssetById(assetId);
            if (assetOpt.isEmpty()) {
                System.out.println("❌ Asset not found.");
                return;
            }

            Asset asset = assetOpt.get();
            
            // Check if asset has depreciation schedules
            List<DepreciationSchedule> schedules = depreciationService.getDepreciationSchedulesForAsset(assetId);
            if (!schedules.isEmpty()) {
                System.out.printf("⚠️  Warning: Asset '%s' has %d depreciation schedule(s) that will also be deleted.%n", 
                    asset.getAssetName(), schedules.size());
                System.out.print("Do you want to continue deleting? (y/N): ");
                String continueDeleting = inputHandler.getString("Continue with deletion");
                if (!continueDeleting.toLowerCase().startsWith("y")) {
                    System.out.println("❌ Deletion cancelled.");
                    return;
                }
                
                // Second confirmation
                System.out.printf("Are you sure you want to delete asset '%s' and all its depreciation schedules? (y/N): ", asset.getAssetName());
                String finalConfirm = inputHandler.getString("Final confirmation");
                if (!finalConfirm.toLowerCase().startsWith("y")) {
                    System.out.println("❌ Deletion cancelled.");
                    return;
                }
            }
            
            depreciationService.deleteAsset(assetId);
            System.out.println("✅ Asset deleted successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error deleting asset: " + e.getMessage());
        }
    }

    private void repostDepreciationSchedules() {
        System.out.println("\n=== Repost Depreciation Schedules ===");
        System.out.println("This will repost all calculated depreciation schedules with correct fiscal periods.");
        System.out.print("Do you want to continue? (y/N): ");
        
        try {
            String confirm = inputHandler.getString("Confirm reposting");
            if (!confirm.toLowerCase().startsWith("y")) {
                System.out.println("❌ Operation cancelled.");
                return;
            }
            
            Long companyId = applicationState.getCurrentCompany().getId();
            depreciationService.repostDepreciationSchedules(companyId);
            System.out.println("✅ Depreciation schedules reposted successfully!");
            
        } catch (Exception e) {
            System.out.println("❌ Error reposting depreciation schedules: " + e.getMessage());
        }
    }

    private void displayQuickCalculationMenu() {
        while (true) {
            System.out.println("\n=== Quick Depreciation Calculation ===");
            System.out.println("1. Calculate Straight-Line Depreciation");
            System.out.println("2. Calculate Declining Balance Depreciation");
            System.out.println("3. Calculate FIN Depreciation");
            System.out.println("4. Compare Depreciation Methods");
            System.out.println("5. Back to Depreciation Menu");
            System.out.print("Enter your choice (1-5): ");

            int choice = inputHandler.getInteger("Enter your choice");

            switch (choice) {
                case 1:
                    calculateStraightLineDepreciation();
                    break;
                case 2:
                    calculateDecliningBalanceDepreciation();
                    break;
                case 3:
                    calculateFINDepreciation();
                    break;
                case 4:
                    compareDepreciationMethods();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("❌ Invalid choice. Please try again.");
            }
        }
    }

    private DepreciationMethod selectDepreciationMethod() {
        System.out.println("Select depreciation method:");
        System.out.println("1. Straight-Line");
        System.out.println("2. Declining Balance");
        System.out.println("3. FIN (5 or 7 years only)");

        while (true) {
            int methodChoice = inputHandler.getInteger("Enter method choice");
            switch (methodChoice) {
                case 1: return DepreciationMethod.STRAIGHT_LINE;
                case 2: return DepreciationMethod.DECLINING_BALANCE;
                case 3: return DepreciationMethod.FIN;
                default:
                    System.out.println("❌ Invalid choice. Please select 1-3.");
            }
        }
    }

    private void calculateStraightLineDepreciation() {
        System.out.println("\n=== Straight-Line Depreciation Calculator ===");

        try {
            BigDecimal cost = inputHandler.getBigDecimal("Enter asset cost");
            BigDecimal salvageValue = inputHandler.getBigDecimal("Enter salvage value");
            int usefulLife = inputHandler.getInteger("Enter useful life (years)");

            DepreciationRequest request = new DepreciationRequest()
                .cost(cost)
                .salvageValue(salvageValue)
                .usefulLife(usefulLife)
                .method(DepreciationMethod.STRAIGHT_LINE);

            DepreciationSchedule schedule = depreciationService.calculateDepreciation(request);

            displayDepreciationSchedule(schedule, "Straight-Line Depreciation");

        } catch (InputHandler.InputCancelledException e) {
            System.out.println("❌ Operation cancelled. Returning to menu.");
        } catch (Exception e) {
            System.out.println("❌ Error calculating depreciation: " + e.getMessage());
        }
    }

    private void calculateDecliningBalanceDepreciation() {
        System.out.println("\n=== Declining Balance Depreciation Calculator ===");

        try {
            BigDecimal cost = inputHandler.getBigDecimal("Enter asset cost");
            BigDecimal salvageValue = inputHandler.getBigDecimal("Enter salvage value");
            int usefulLife = inputHandler.getInteger("Enter useful life (years)");

            // Display rate options
            System.out.println("\nSelect declining balance rate:");
            System.out.println("1. 20%");
            System.out.println("2. 25%");
            System.out.println("3. 30%");
            System.out.println("4. 33.33%");
            System.out.println("5. 35%");

            int choice = inputHandler.getInteger("Enter your choice (1-5)");
            BigDecimal selectedRate;
            switch (choice) {
                case 1: selectedRate = BigDecimal.valueOf(20); break;
                case 2: selectedRate = BigDecimal.valueOf(25); break;
                case 3: selectedRate = BigDecimal.valueOf(30); break;
                case 4: selectedRate = BigDecimal.valueOf(33.33); break;
                case 5: selectedRate = BigDecimal.valueOf(35); break;
                default:
                    System.out.println("❌ Invalid choice. Using 20% as default.");
                    selectedRate = BigDecimal.valueOf(20);
                }

                // Calculate factor: factor = rate / 100 (annual percentage rate)
                BigDecimal dbFactor = selectedRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

                System.out.printf("Selected rate: %.2f%%, Calculated factor: %.2f%n", selectedRate, dbFactor);

                DepreciationRequest request = new DepreciationRequest()
                .cost(cost)
                .salvageValue(salvageValue)
                .usefulLife(usefulLife)
                .method(DepreciationMethod.DECLINING_BALANCE)
                .dbFactor(dbFactor);

            DepreciationSchedule schedule = depreciationService.calculateDepreciation(request);

            displayDepreciationSchedule(schedule, "Declining Balance Depreciation");

        } catch (InputHandler.InputCancelledException e) {
            System.out.println("❌ Operation cancelled. Returning to menu.");
        } catch (Exception e) {
            System.out.println("❌ Error calculating depreciation: " + e.getMessage());
        }
    }

    private void calculateFINDepreciation() {
        System.out.println("\n=== FIN Depreciation Calculator ===");
        System.out.println("Supported recovery periods: 5, 7 years");

        try {
            BigDecimal cost = inputHandler.getBigDecimal("Enter asset cost");
            int recoveryPeriod = inputHandler.getInteger("Enter recovery period (5 or 7 years)");

            // Validate recovery period
            if (recoveryPeriod != FIN_RECOVERY_PERIOD_5 && recoveryPeriod != FIN_RECOVERY_PERIOD_7) {
                System.out.println("❌ FIN method only supports 5 or 7 year recovery periods.");
                return;
            }

            DepreciationRequest request = new DepreciationRequest()
                .cost(cost)
                .salvageValue(ZERO_SALVAGE) // FIN typically uses zero salvage
                .usefulLife(recoveryPeriod)
                .method(DepreciationMethod.FIN);

            DepreciationSchedule schedule = depreciationService.calculateDepreciation(request);

            displayDepreciationSchedule(schedule, "FIN Depreciation");

        } catch (InputHandler.InputCancelledException e) {
            System.out.println("❌ Operation cancelled. Returning to menu.");
        } catch (Exception e) {
            System.out.println("❌ Error calculating depreciation: " + e.getMessage());
        }
    }

    private void compareDepreciationMethods() {
        System.out.println("\n=== Compare Depreciation Methods ===");

        try {
            BigDecimal cost = inputHandler.getBigDecimal("Enter asset cost");
            BigDecimal salvageValue = inputHandler.getBigDecimal("Enter salvage value");
            int usefulLife = inputHandler.getInteger("Enter useful life (years)");

            // Display rate options
            System.out.println("\nSelect declining balance rate for comparison:");
            System.out.println("1. 20%");
            System.out.println("2. 25%");
            System.out.println("3. 30%");
            System.out.println("4. 33.33%");
            System.out.println("5. 35%");

            int choice = inputHandler.getInteger("Enter your choice (1-5)");
            BigDecimal selectedRate;
            switch (choice) {
                case 1: selectedRate = BigDecimal.valueOf(20); break;
                case 2: selectedRate = BigDecimal.valueOf(25); break;
                case 3: selectedRate = BigDecimal.valueOf(30); break;
                case 4: selectedRate = BigDecimal.valueOf(33.33); break;
                case 5: selectedRate = BigDecimal.valueOf(35); break;
                default:
                    System.out.println("❌ Invalid choice. Using 20% as default.");
                    selectedRate = BigDecimal.valueOf(20);
            }

            // Calculate factor: factor = rate / 100 (annual percentage rate)
            BigDecimal dbFactor = selectedRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

            displayComparisonHeader(cost, salvageValue, usefulLife, selectedRate);

            // Calculate schedules
            DepreciationSchedule slSchedule = calculateStraightLineSchedule(cost, salvageValue, usefulLife);
            DepreciationSchedule dbSchedule = calculateDecliningBalanceSchedule(cost, salvageValue, usefulLife, dbFactor);

            displayComparisonTable(slSchedule, dbSchedule, usefulLife);

        } catch (InputHandler.InputCancelledException e) {
            System.out.println("❌ Operation cancelled. Returning to menu.");
        } catch (Exception e) {
            System.out.println("❌ Error comparing depreciation methods: " + e.getMessage());
        }
    }

    private void displayComparisonHeader(BigDecimal cost, BigDecimal salvageValue, int usefulLife, BigDecimal selectedRate) {
        System.out.println("\n" + "=".repeat(TABLE_WIDTH));
        System.out.println("DEPRECIATION METHOD COMPARISON");
        System.out.println("=".repeat(TABLE_WIDTH));
        System.out.printf("Asset Cost: R%.2f%n", cost);
        System.out.printf("Salvage Value: R%.2f%n", salvageValue);
        System.out.printf("Useful Life: %d years%n", usefulLife);
        System.out.printf("DB Rate: %.2f%%%n", selectedRate);
        System.out.println("=".repeat(TABLE_WIDTH));
    }

    private DepreciationSchedule calculateStraightLineSchedule(BigDecimal cost, BigDecimal salvageValue, int usefulLife) {
        DepreciationRequest slRequest = new DepreciationRequest()
            .cost(cost)
            .salvageValue(salvageValue)
            .usefulLife(usefulLife)
            .method(DepreciationMethod.STRAIGHT_LINE);

        return depreciationService.calculateDepreciation(slRequest);
    }

    private DepreciationSchedule calculateDecliningBalanceSchedule(BigDecimal cost, BigDecimal salvageValue, int usefulLife, BigDecimal dbFactor) {
        DepreciationRequest dbRequest = new DepreciationRequest()
            .cost(cost)
            .salvageValue(salvageValue)
            .usefulLife(usefulLife)
            .method(DepreciationMethod.DECLINING_BALANCE)
            .dbFactor(dbFactor);

        return depreciationService.calculateDepreciation(dbRequest);
    }

    private void displayComparisonTable(DepreciationSchedule slSchedule, DepreciationSchedule dbSchedule, int usefulLife) {
        // Display comparison table
        System.out.println("Year | Straight-Line | Declining Balance | SL Cumulative | DB Cumulative | SL Book Value | DB Book Value");
        System.out.println("-".repeat(COMPARISON_TABLE_WIDTH));

        for (int year = 1; year <= usefulLife; year++) {
            BigDecimal slDep = slSchedule.getYears().get(year - 1).getDepreciation();
            BigDecimal dbDep = dbSchedule.getYears().get(year - 1).getDepreciation();
            BigDecimal slCum = slSchedule.getYears().get(year - 1).getCumulativeDepreciation();
            BigDecimal dbCum = dbSchedule.getYears().get(year - 1).getCumulativeDepreciation();
            BigDecimal slBook = slSchedule.getYears().get(year - 1).getBookValue();
            BigDecimal dbBook = dbSchedule.getYears().get(year - 1).getBookValue();

            System.out.printf("%4d | %12.2f | %16.2f | %12.2f | %14.2f | %12.2f | %12.2f%n",
                year, slDep, dbDep, slCum, dbCum, slBook, dbBook);
        }

        System.out.println("=".repeat(COMPARISON_TABLE_WIDTH));
        System.out.printf("Total Depreciation - SL: R%.2f, DB: R%.2f%n",
            slSchedule.getTotalDepreciation(), dbSchedule.getTotalDepreciation());
        System.out.printf("Final Book Value - SL: R%.2f, DB: R%.2f%n",
            slSchedule.getFinalBookValue(), dbSchedule.getFinalBookValue());
    }

    private void displayDepreciationSchedule(DepreciationSchedule schedule, String methodName) {
        System.out.println("\n" + "=".repeat(TABLE_WIDTH));
        System.out.println(methodName.toUpperCase() + " SCHEDULE");
        System.out.println("=".repeat(TABLE_WIDTH));

        System.out.println("Year | Annual Depreciation | Cumulative Depreciation | Book Value");
        System.out.println("-".repeat(SCHEDULE_TABLE_WIDTH));

        for (DepreciationYear year : schedule.getYears()) {
            System.out.printf("%4d | %18.2f | %22.2f | %10.2f%n",
                year.getYear(),
                year.getDepreciation(),
                year.getCumulativeDepreciation(),
                year.getBookValue());
        }

        System.out.println("=".repeat(SCHEDULE_TABLE_WIDTH));
        System.out.printf("Total Depreciation: R%.2f%n", schedule.getTotalDepreciation());
        System.out.printf("Final Book Value: R%.2f%n", schedule.getFinalBookValue());
    }
}


