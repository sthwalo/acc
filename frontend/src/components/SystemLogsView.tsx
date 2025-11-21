import { useState, useEffect, useRef, useCallback } from 'react';
import { FileText, Download, Search, AlertCircle, Info, CheckCircle, XCircle } from 'lucide-react';
import type { Company } from '../types/api';

interface SystemLogsViewProps {
  selectedCompany: Company;
  onLogout?: () => void;
}

interface LogEntry {
  id: number;
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  category: string;
  message: string;
  user_id?: number;
  company_id?: number;
  details?: string;
}

export default function SystemLogsView({ selectedCompany, onLogout }: SystemLogsViewProps) {
  const INACTIVITY_TIMEOUT = 5 * 60 * 1000; // 5 minutes in milliseconds
  
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [filteredLogs, setFilteredLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [levelFilter, setLevelFilter] = useState<string>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [dateRange, setDateRange] = useState<string>('24h');
  const [lastActivity, setLastActivity] = useState<number>(Date.now());
  const inactivityTimerRef = useRef<number | null>(null);
  const [countdown, setCountdown] = useState<number>(Math.round(INACTIVITY_TIMEOUT / 60000));

  // Update countdown every minute
  useEffect(() => {
    const countdownInterval = setInterval(() => {
      const remaining = Math.max(0, Math.round((INACTIVITY_TIMEOUT - (Date.now() - lastActivity)) / 60000));
      setCountdown(remaining);
    }, 60000); // Update every minute

    return () => clearInterval(countdownInterval);
  }, [lastActivity, INACTIVITY_TIMEOUT]);
  const updateActivity = useCallback(() => {
    setLastActivity(Date.now());
  }, []);

  // Check for inactivity and auto-logout
  const checkInactivity = useCallback(() => {
    const now = Date.now();
    if (now - lastActivity > INACTIVITY_TIMEOUT) {
      // Add logout log entry
      const logoutLog: LogEntry = {
        id: Date.now(), // Use timestamp as ID for uniqueness
        timestamp: new Date().toISOString(),
        level: 'INFO',
        category: 'SESSION',
        message: 'User automatically logged out due to inactivity',
        user_id: 1, // Assuming current user
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: `Session expired after ${Math.round(INACTIVITY_TIMEOUT / 60000)} minutes of inactivity`
      };

      setLogs(prevLogs => [logoutLog, ...prevLogs]);

      // Call logout callback if provided
      if (onLogout) {
        onLogout();
      }
    }
  }, [lastActivity, INACTIVITY_TIMEOUT, selectedCompany, onLogout]);

  // Set up activity listeners and inactivity timer
  useEffect(() => {
    // Activity event listeners
    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
    
    events.forEach(event => {
      document.addEventListener(event, updateActivity, true);
    });

    // Set up inactivity check timer
    inactivityTimerRef.current = setInterval(checkInactivity, 30000); // Check every 30 seconds

    // Cleanup
    return () => {
      events.forEach(event => {
        document.removeEventListener(event, updateActivity, true);
      });
      
      if (inactivityTimerRef.current) {
        clearInterval(inactivityTimerRef.current);
      }
    };
  }, [updateActivity, checkInactivity]);

  // Mock data for demonstration - in real implementation, this would come from API
  useEffect(() => {
    const mockLogs: LogEntry[] = [
      {
        id: 1,
        timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
        level: 'INFO',
        category: 'SESSION',
        message: 'User session started',
        user_id: 1,
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: 'Login from frontend application'
      },
      {
        id: 2,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(),
        level: 'INFO',
        category: 'TRANSACTION_PROCESSING',
        message: 'Bank statement processed successfully',
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: 'Processed 45 transactions from ABC Bank statement'
      },
      {
        id: 3,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(),
        level: 'WARN',
        category: 'DATA_VALIDATION',
        message: 'Duplicate transaction detected',
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: 'Transaction REF-123 already exists, skipping duplicate'
      },
      {
        id: 4,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 6).toISOString(),
        level: 'ERROR',
        category: 'DATABASE',
        message: 'Connection timeout during report generation',
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: 'Failed to generate financial report due to database timeout'
      },
      {
        id: 5,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 8).toISOString(),
        level: 'INFO',
        category: 'PAYROLL',
        message: 'Payroll processing completed',
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: 'Processed payroll for 12 employees, total gross: R 150,000.00'
      },
      {
        id: 6,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString(),
        level: 'INFO',
        category: 'BACKUP',
        message: 'Database backup completed successfully',
        details: 'Backup saved to: /backups/drimacc_db_backup_2025-11-07.dump'
      },
      {
        id: 7,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 18).toISOString(),
        level: 'DEBUG',
        category: 'API',
        message: 'API endpoint accessed',
        user_id: 1,
        company_id: selectedCompany?.id ? Number(selectedCompany.id) : undefined,
        details: 'GET /api/v1/companies/1/fiscal-periods - Status: 200'
      },
      {
        id: 8,
        timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
        level: 'INFO',
        category: 'SYSTEM',
        message: 'System maintenance completed',
        details: 'Cleaned up temporary files and optimized database indexes'
      }
    ];

    // Simulate API call delay
    setTimeout(() => {
      setLogs(mockLogs);
      setFilteredLogs(mockLogs);
      setLoading(false);
    }, 1000);
  }, [selectedCompany]);

  // Filter logs based on search term, level, and category
  useEffect(() => {
    let filtered = logs;

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(log =>
        log.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
        log.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (log.details && log.details.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    // Filter by level
    if (levelFilter !== 'ALL') {
      filtered = filtered.filter(log => log.level === levelFilter);
    }

    // Filter by category
    if (categoryFilter !== 'ALL') {
      filtered = filtered.filter(log => log.category === categoryFilter);
    }

    // Filter by date range
    const now = new Date();
    const rangeMs = {
      '1h': 1000 * 60 * 60,
      '24h': 1000 * 60 * 60 * 24,
      '7d': 1000 * 60 * 60 * 24 * 7,
      '30d': 1000 * 60 * 60 * 24 * 30
    }[dateRange] || 1000 * 60 * 60 * 24;

    filtered = filtered.filter(log => {
      const logTime = new Date(log.timestamp).getTime();
      return (now.getTime() - logTime) <= rangeMs;
    });

    setFilteredLogs(filtered);
  }, [logs, searchTerm, levelFilter, categoryFilter, dateRange]);

  const getLogIcon = (level: string) => {
    switch (level) {
      case 'ERROR':
        return <XCircle size={16} className="log-icon error" />;
      case 'WARN':
        return <AlertCircle size={16} className="log-icon warning" />;
      case 'INFO':
        return <Info size={16} className="log-icon info" />;
      case 'DEBUG':
        return <CheckCircle size={16} className="log-icon debug" />;
      default:
        return <Info size={16} className="log-icon" />;
    }
  };

  const getUniqueCategories = () => {
    const categories = logs.map(log => log.category);
    return ['ALL', ...Array.from(new Set(categories))];
  };

  const exportLogs = () => {
    const csvContent = [
      ['Timestamp', 'Level', 'Category', 'Message', 'Details'].join(','),
      ...filteredLogs.map(log => [
        log.timestamp,
        log.level,
        log.category,
        `"${log.message}"`,
        `"${log.details || ''}"`
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `system_logs_${selectedCompany?.name}_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="system-logs-view">
        <div className="view-header">
          <h2>System Logs</h2>
          <p>Loading logs for {selectedCompany?.name}...</p>
          <div className={`session-info ${countdown <= 1 ? 'warning' : ''}`}>
            <small>Session active - Auto-logout in {countdown} minute{countdown !== 1 ? 's' : ''}</small>
          </div>
        </div>
        <div className="loading-spinner">Loading...</div>
      </div>
    );
  }

  return (
    <div className="system-logs-view">
      <div className="view-header">
        <h2>System Logs</h2>
        <p>Audit trail and system events for {selectedCompany?.name}</p>
        <div className={`session-info ${countdown <= 1 ? 'warning' : ''}`}>
          <small>
            Session active - Auto-logout in {countdown} minute{countdown !== 1 ? 's' : ''}
            {countdown <= 1 && ' - Session expiring soon!'}
          </small>
        </div>
      </div>

      {/* Filters */}
      <div className="logs-filters">
        <div className="filter-group">
          <div className="search-input">
            <Search size={16} />
            <input
              type="text"
              placeholder="Search logs..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <select
            value={levelFilter}
            onChange={(e) => setLevelFilter(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">All Levels</option>
            <option value="ERROR">Errors</option>
            <option value="WARN">Warnings</option>
            <option value="INFO">Info</option>
            <option value="DEBUG">Debug</option>
          </select>

          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="filter-select"
          >
            {getUniqueCategories().map(category => (
              <option key={category} value={category}>
                {category === 'ALL' ? 'All Categories' : category}
              </option>
            ))}
          </select>

          <select
            value={dateRange}
            onChange={(e) => setDateRange(e.target.value)}
            className="filter-select"
          >
            <option value="1h">Last Hour</option>
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
          </select>

          <button className="action-button secondary" onClick={exportLogs}>
            <Download size={16} />
            Export CSV
          </button>
        </div>
      </div>

      {/* Logs List */}
      <div className="logs-container">
        <div className="logs-header">
          <span>{filteredLogs.length} log entries</span>
        </div>

        <div className="logs-list">
          {filteredLogs.length === 0 ? (
            <div className="no-logs">
              <FileText size={48} />
              <p>No logs found matching your filters</p>
            </div>
          ) : (
            filteredLogs.map((log) => (
              <div key={log.id} className={`log-entry log-${log.level.toLowerCase()}`}>
                <div className="log-header">
                  {getLogIcon(log.level)}
                  <span className="log-timestamp">
                    {new Date(log.timestamp).toLocaleString('en-ZA')}
                  </span>
                  <span className="log-level">{log.level}</span>
                  <span className="log-category">{log.category}</span>
                </div>
                <div className="log-message">{log.message}</div>
                {log.details && (
                  <div className="log-details">{log.details}</div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}