import { useState, useEffect } from 'react';
import { Clock, RefreshCw } from 'lucide-react';
import type { Company } from '../types/api';

interface CurrentTimeViewProps {
  selectedCompany: Company;
}

export default function CurrentTimeView({ selectedCompany }: CurrentTimeViewProps) {
  const [currentTime, setCurrentTime] = useState<Date>(new Date());
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    // Update time every second
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const refreshTime = () => {
    setIsRefreshing(true);
    setCurrentTime(new Date());

    // Show refresh animation for 500ms
    setTimeout(() => {
      setIsRefreshing(false);
    }, 500);
  };

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('en-ZA', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const formatDate = (date: Date) => {
    return date.toLocaleDateString('en-ZA', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatDateTime = (date: Date) => {
    return date.toLocaleString('en-ZA', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  };

  return (
    <div className="current-time-view">
      <div className="view-header">
        <h2>Current System Time</h2>
        <p>Real-time clock for {selectedCompany?.name}</p>
      </div>

      <div className="time-display-container">
        <div className="time-card">
          <div className="time-header">
            <Clock size={24} />
            <h3>Current Time</h3>
            <button
              className={`refresh-button ${isRefreshing ? 'refreshing' : ''}`}
              onClick={refreshTime}
              title="Refresh time"
            >
              <RefreshCw size={16} />
            </button>
          </div>

          <div className="time-content">
            <div className="time-display">
              <div className="time-value">{formatTime(currentTime)}</div>
              <div className="date-value">{formatDate(currentTime)}</div>
            </div>

            <div className="time-details">
              <div className="detail-item">
                <span className="label">Full Date & Time:</span>
                <span className="value">{formatDateTime(currentTime)}</span>
              </div>
              <div className="detail-item">
                <span className="label">Timezone:</span>
                <span className="value">South Africa Standard Time (SAST)</span>
              </div>
              <div className="detail-item">
                <span className="label">Unix Timestamp:</span>
                <span className="value">{Math.floor(currentTime.getTime() / 1000)}</span>
              </div>
              <div className="detail-item">
                <span className="label">ISO 8601:</span>
                <span className="value">{currentTime.toISOString()}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="time-info">
          <h4>System Information</h4>
          <div className="info-grid">
            <div className="info-item">
              <span className="label">Company:</span>
              <span className="value">{selectedCompany?.name}</span>
            </div>
            <div className="info-item">
              <span className="label">Registration:</span>
              <span className="value">{selectedCompany?.registrationNumber}</span>
            </div>
            <div className="info-item">
              <span className="label">Tax Number:</span>
              <span className="value">{selectedCompany?.taxNumber}</span>
            </div>
            <div className="info-item">
              <span className="label">VAT Registered:</span>
              <span className="value">{selectedCompany?.vatRegistered ? 'Yes' : 'No'}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}