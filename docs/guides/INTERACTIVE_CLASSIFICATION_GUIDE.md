# Interactive Classification System User Guide

## Overview

The Interactive Classification System is a unified interface for categorizing bank transactions. It provides an intelligent, rule-based approach to transaction classification with an intuitive user interface.

## Getting Started

To start the classification system, run:

```
java -jar app/build/libs/fin-spring.jar
```

## Key Features

### 1. Intelligent Classification

The system uses pattern recognition and machine learning techniques to automatically classify transactions based on previous classifications. Key features include:

- **Pattern Recognition**: Identifies patterns in transaction descriptions
- **Confidence Scoring**: Provides a confidence score for suggested classifications
- **Learning Capability**: Improves over time as more transactions are classified
- **Auto-categorization**: Can apply rules to similar transactions automatically

### 2. Interactive User Interface

The user interface is designed to be intuitive and efficient:

- **Transaction Details**: Clear display of transaction information
- **Similar Transactions**: Shows similar previously classified transactions
- **Account Suggestions**: Provides context-relevant account suggestions
- **Navigation Menus**: Easy-to-use menu system for different operations

### 3. Rule Management

The system maintains a database of classification rules:

- **Rule Creation**: Automatically creates rules from manual classifications
- **Rule Refinement**: Improves rules based on usage
- **Rule Application**: Applies rules consistently across similar transactions

## Workflow

### 1. Select Company and Fiscal Period

When you start the system, you'll be prompted to select a company and fiscal period to work with.

### 2. Main Menu

The main menu offers the following options:

- **Review Uncategorized Transactions**: Process transactions that haven't been categorized
- **Analyze Account Allocations**: View distribution of transactions across accounts
- **Show Categorization Summary**: Get statistics about classification progress
- **Save Changes**: Save your work
- **Exit**: Exit the system
- **Show Change History**: View changes made in the current session

### 3. Classifying Transactions

When classifying a transaction:

1. View the transaction details
2. See similar transactions for context
3. Enter an account code and name, or accept a suggestion
4. Optionally apply the classification to similar transactions

## Tips for Efficient Use

1. **Auto-classification**: Use auto-classification for recurring transactions to save time
2. **Regular Saving**: Save your work regularly to prevent data loss
3. **Pattern Analysis**: Review the Account Allocations to identify patterns in your data
4. **Consistency**: Use consistent account codes for similar transactions

## Keyboard Shortcuts

- `y/n` - Yes/No for confirmation prompts
- `q` - Quit current operation
- Number keys - Select menu options

## Support

For additional support, refer to the system documentation or contact your system administrator.
