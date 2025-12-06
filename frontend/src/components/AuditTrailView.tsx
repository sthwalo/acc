import { useState, useEffect, useCallback } from 'react';
import { BookOpen, Search, Calendar, ChevronLeft, ChevronRight, Loader, AlertCircle, Printer, Download } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import JournalEntryDetailModal from './JournalEntryDetailModal';
import type { Company, FiscalPeriod, JournalEntryDTO, AuditTrailResponse } from '../types/api';

interface AuditTrailViewProps {
  selectedCompany: Company;
  selectedPeriod: FiscalPeriod;
  onClose: () => void;
}

export default function AuditTrailView({ selectedCompany, selectedPeriod, onClose }: AuditTrailViewProps) {
  const api = useApi();
  const [auditTrailData, setAuditTrailData] = useState<AuditTrailResponse | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(20);
  
  // Filter state
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [appliedFilters, setAppliedFilters] = useState({
    startDate: '',
    endDate: '',
    searchTerm: ''
  });

  const fetchAuditTrail = useCallback(async () => {
    if (!selectedCompany || !selectedPeriod) return;

    setIsLoading(true);
    setError(null);

    try {
      const response = await api.reports.getAuditTrail(
        selectedCompany.id,
        selectedPeriod.id,
        currentPage,
        pageSize,
        appliedFilters.startDate || undefined,
        appliedFilters.endDate || undefined,
        appliedFilters.searchTerm || undefined
      );

      // API returns AuditTrailResponse directly, not wrapped
      setAuditTrailData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An unexpected error occurred');
    } finally {
      setIsLoading(false);
    }
  }, [api, selectedCompany, selectedPeriod, currentPage, pageSize, appliedFilters]);

  useEffect(() => {
    fetchAuditTrail();
  }, [fetchAuditTrail]);

  const handleApplyFilters = () => {
    setAppliedFilters({
      startDate,
      endDate,
      searchTerm
    });
    setCurrentPage(0); // Reset to first page when filters change
  };

  const handleClearFilters = () => {
    setStartDate('');
    setEndDate('');
    setSearchTerm('');
    setAppliedFilters({
      startDate: '',
      endDate: '',
      searchTerm: ''
    });
    setCurrentPage(0);
  };

  const handlePageChange = (newPage: number) => {
    setCurrentPage(newPage);
  };

  const handleRowClick = (entryId: number) => {
    setSelectedEntry(entryId);
  };

  const handleCloseModal = () => {
    setSelectedEntry(null);
  };

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString('en-ZA', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const handlePrint = () => {
    window.print();
  };

  const handleDownloadCSV = () => {
    if (!auditTrailData || auditTrailData.entries.length === 0) {
      setError('No data to download');
      return;
    }

    // Create CSV content
    const headers = ['Date', 'Reference', 'Description', 'Debit Amount', 'Credit Amount', 'Created By'];
    const csvRows = [headers.join(',')];

    auditTrailData.entries.forEach((entry: JournalEntryDTO) => {
      const row = [
        formatDate(entry.date),
        entry.reference || '',
        `"${entry.description?.replace(/"/g, '""') || ''}"`, // Escape quotes in description
        formatCurrency(entry.totalDebit),
        formatCurrency(entry.totalCredit),
        entry.createdBy || ''
      ];
      csvRows.push(row.join(','));
    });

    // Add summary row
    csvRows.push('');
    csvRows.push(`Total Entries,${auditTrailData.totalElements}`);
    csvRows.push(`Total Debits,${formatCurrency(auditTrailData.entries.reduce((sum: number, e: JournalEntryDTO) => sum + e.totalDebit, 0))}`);
    csvRows.push(`Total Credits,${formatCurrency(auditTrailData.entries.reduce((sum: number, e: JournalEntryDTO) => sum + e.totalCredit, 0))}`);

    // Create blob and download
    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    const fileName = `AuditTrail_${selectedCompany.name}_${selectedPeriod.periodName}_${new Date().toISOString().split('T')[0]}.csv`;
    link.setAttribute('href', url);
    link.setAttribute('download', fileName);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="audit-trail-view">
      {/* Header */}
      <div className="audit-trail-header">
        <div className="header-left">
          <BookOpen className="header-icon" size={24} />
          <div>
            <h2>Audit Trail</h2>
            <p className="header-subtitle">
              {selectedCompany.name} - {selectedPeriod.periodName}
            </p>
          </div>
        </div>
        <div className="header-actions">
          <button
            onClick={handlePrint}
            disabled={isLoading || !auditTrailData || auditTrailData.entries.length === 0}
            className="action-button print-button"
            title="Print audit trail"
          >
            <Printer size={18} />
            Print
          </button>
          <button
            onClick={handleDownloadCSV}
            disabled={isLoading || !auditTrailData || auditTrailData.entries.length === 0}
            className="action-button download-button"
            title="Download as CSV"
          >
            <Download size={18} />
            Download
          </button>
          <button
            onClick={onClose}
            className="close-button"
          >
            Close
          </button>
        </div>
      </div>

      {/* API Message Banner */}
      {error && (
        <ApiMessageBanner
          message={error}
          type="error"
        />
      )}

      {/* Filters */}
      <div className="audit-trail-filters">
        <div className="filters-grid">
          {/* Start Date Filter */}
          <div className="filter-group">
            <label>Start Date</label>
            <div className="input-with-icon">
              <Calendar className="input-icon" size={18} />
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                placeholder="Start date"
              />
            </div>
          </div>

          {/* End Date Filter */}
          <div className="filter-group">
            <label>End Date</label>
            <div className="input-with-icon">
              <Calendar className="input-icon" size={18} />
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                placeholder="End date"
              />
            </div>
          </div>

          {/* Search Filter */}
          <div className="filter-group">
            <label>Search</label>
            <div className="input-with-icon">
              <Search className="input-icon" size={18} />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search reference or description..."
              />
            </div>
          </div>
        </div>

        {/* Filter Buttons */}
        <div className="filter-actions">
          <button
            onClick={handleApplyFilters}
            disabled={isLoading}
            className="apply-button"
          >
            Apply Filters
          </button>
          <button
            onClick={handleClearFilters}
            disabled={isLoading}
            className="clear-button"
          >
            Clear Filters
          </button>
        </div>

        {/* Active Filters Display */}
        {(appliedFilters.startDate || appliedFilters.endDate || appliedFilters.searchTerm) && (
          <div className="active-filters">
            <p className="active-filters-label">Active Filters:</p>
            <div className="filter-tags">
              {appliedFilters.startDate && (
                <span className="filter-tag">
                  From: {formatDate(appliedFilters.startDate)}
                </span>
              )}
              {appliedFilters.endDate && (
                <span className="filter-tag">
                  To: {formatDate(appliedFilters.endDate)}
                </span>
              )}
              {appliedFilters.searchTerm && (
                <span className="filter-tag">
                  Search: "{appliedFilters.searchTerm}"
                </span>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="loading-state">
          <Loader className="spinner-icon" size={32} />
          <span>Loading audit trail...</span>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && auditTrailData && auditTrailData.entries.length === 0 && (
        <div className="empty-state">
          <AlertCircle size={48} />
          <p className="empty-title">No journal entries found</p>
          <p className="empty-subtitle">Try adjusting your filters or date range</p>
        </div>
      )}

      {/* Data Table */}
      {!isLoading && auditTrailData && auditTrailData.entries.length > 0 && (
        <>
          <div className="audit-trail-table-container">
            <table className="audit-trail-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Date</th>
                  <th>Description</th>
                  <th className="text-right">Debit</th>
                  <th className="text-right">Credit</th>
                  <th className="text-center">Lines</th>
                  <th>Created By</th>
                </tr>
              </thead>
              <tbody>
                {auditTrailData.entries.map((entry: JournalEntryDTO) => (
                  <tr
                    key={entry.id}
                    onClick={() => handleRowClick(entry.id)}
                    className="table-row-clickable"
                  >
                    <td className="reference-cell">{entry.reference}</td>
                    <td>{formatDate(entry.entryDate)}</td>
                    <td>{entry.description}</td>
                    <td className="text-right amount-cell">{formatCurrency(entry.totalDebit)}</td>
                    <td className="text-right amount-cell">{formatCurrency(entry.totalCredit)}</td>
                    <td className="text-center">{entry.lineCount}</td>
                    <td>{entry.createdBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {auditTrailData.pagination && (
            <div className="pagination-container">
              <div className="pagination-info">
                Showing page {auditTrailData.pagination.currentPage + 1} of{' '}
                {auditTrailData.pagination.totalPages} ({auditTrailData.pagination.totalElements} total entries)
              </div>
              <div className="pagination-controls">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0 || isLoading}
                  className="pagination-button"
                >
                  <ChevronLeft size={20} />
                </button>
                <div className="pagination-page">
                  <span>Page {currentPage + 1}</span>
                </div>
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= auditTrailData.pagination.totalPages - 1 || isLoading}
                  className="pagination-button"
                >
                  <ChevronRight size={20} />
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Detail Modal */}
      {selectedEntry && (
        <JournalEntryDetailModal
          entryId={selectedEntry}
          onClose={handleCloseModal}
        />
      )}
    </div>
  );
}
