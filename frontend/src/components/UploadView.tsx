import { useState, useRef } from 'react';
import { Upload, FileText, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import type { Company, UploadResponse, FiscalPeriod } from '../types/api';

interface UploadViewProps {
  selectedCompany: Company;
  selectedFiscalPeriod?: FiscalPeriod;
}

export default function UploadView({ selectedCompany, selectedFiscalPeriod }: UploadViewProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [processingStatus, setProcessingStatus] = useState<string>('');
  const [uploadResult, setUploadResult] = useState<UploadResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['application/pdf', 'text/csv', 'text/plain'];
      if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.pdf')) {
        setError('Please select a PDF, CSV, or text file');
        return;
      }

      // Validate file size (max 10MB)
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        setError('File size must be less than 10MB');
        return;
      }

      setSelectedFile(file);
      setError(null);
      setUploadResult(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    if (!selectedFiscalPeriod) {
      setError('Please select a fiscal period first');
      return;
    }

    try {
      setUploading(true);
      setError(null);
      setUploadProgress(0);
      setProcessingStatus('Preparing file...');

      const apiService = serviceRegistry.get<ApiService>('apiService');

      // Simulate progress updates during upload
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => {
          if (prev < 90) {
            setProcessingStatus(prev < 30 ? 'Uploading file...' : prev < 60 ? 'Processing document...' : 'Extracting transactions...');
            return prev + 10;
          }
          return prev;
        });
      }, 1000);

      const result = await apiService.uploadFile(Number(selectedCompany.id), Number(selectedFiscalPeriod.id), selectedFile);

      clearInterval(progressInterval);
      setUploadProgress(100);
      setProcessingStatus('Processing complete!');

      setUploadResult(result);

      // Clear the file selection after successful upload
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }

      // Reset progress after a delay
      setTimeout(() => {
        setUploadProgress(0);
        setProcessingStatus('');
      }, 2000);

    } catch (err) {
      setUploadProgress(0);
      setProcessingStatus('');

      if (err instanceof Error) {
        if (err.message.includes('timeout')) {
          setError('Upload timed out. The file is being processed in the background. Please check back in a few minutes.');
        } else {
          setError(err.message);
        }
      } else {
        setError('Failed to upload file. Please try again.');
      }
      console.error('Upload error:', err);
    } finally {
      setUploading(false);
    }
  };

  const handleDragOver = (event: React.DragEvent) => {
    event.preventDefault();
  };

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault();
    const file = event.dataTransfer.files?.[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['application/pdf', 'text/csv', 'text/plain'];
      if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.pdf')) {
        setError('Please select a PDF, CSV, or text file');
        return;
      }

      // Validate file size (max 10MB)
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        setError('File size must be less than 10MB');
        return;
      }

      setSelectedFile(file);
      setError(null);
      setUploadResult(null);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="upload-view">
      <div className="view-header">
        <h2>Upload Documents - {selectedCompany.name}</h2>
        <p>Upload bank statements, invoices, or other financial documents for processing</p>
      </div>

      <div className="upload-section">
        <div
          className={`upload-dropzone ${selectedFile ? 'has-file' : ''}`}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            id="file-upload"
            name="file-upload"
            accept=".pdf,.csv,.txt"
            onChange={handleFileSelect}
            style={{ display: 'none' }}
          />

          {selectedFile ? (
            <div className="file-selected">
              <FileText size={48} />
              <div className="file-info">
                <h4>{selectedFile.name}</h4>
                <p>{formatFileSize(selectedFile.size)}</p>
                <p className="file-type">{selectedFile.type || 'Unknown type'}</p>
              </div>
            </div>
          ) : (
            <div className="upload-prompt">
              <Upload size={48} />
              <h3>Drop files here or click to browse</h3>
              <p>Supported formats: PDF, CSV, TXT (max 10MB)</p>
              <p className="upload-types">
                Bank statements, invoices, receipts, and other financial documents
              </p>
            </div>
          )}
        </div>

        {error && (
          <div className="error-message">
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
        )}

        <div className="upload-actions">
          <button
            className="upload-button"
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
          >
            {uploading ? (
              <>
                <div className="spinner small"></div>
                {processingStatus || 'Uploading...'}
              </>
            ) : (
              <>
                <Upload size={20} />
                Upload Document
              </>
            )}
          </button>

          {uploading && (
            <div className="upload-progress">
              <div className="progress-bar">
                <div
                  className="progress-fill"
                  style={{ width: `${uploadProgress}%` }}
                ></div>
              </div>
              <span className="progress-text">{uploadProgress}%</span>
            </div>
          )}

          {selectedFile && !uploading && (
            <button
              className="clear-button"
              onClick={() => {
                setSelectedFile(null);
                setError(null);
                setUploadProgress(0);
                setProcessingStatus('');
                if (fileInputRef.current) {
                  fileInputRef.current.value = '';
                }
              }}
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {uploadResult && (
        <div className="upload-result">
          <div className={`result-header ${uploadResult.summary.validTransactions > 0 ? 'success' : 'error'}`}>
            {uploadResult.summary.validTransactions > 0 ? (
              <CheckCircle size={24} />
            ) : (
              <XCircle size={24} />
            )}
            <h3>{uploadResult.summary.validTransactions > 0 ? 'Upload Successful' : 'Upload Failed'}</h3>
          </div>

          <div className="result-details">
            <div className="result-item">
              <span className="label">Files Processed:</span>
              <span className="value">1</span>
            </div>

            <div className="result-item">
              <span className="label">Transactions Found:</span>
              <span className="value">{uploadResult.summary.validTransactions}</span>
            </div>

            <div className="result-item">
              <span className="label">Lines Processed:</span>
              <span className="value">{uploadResult.summary.totalLinesProcessed}</span>
            </div>

            <div className="result-item">
              <span className="label">Errors:</span>
              <span className="value">{uploadResult.errors.length > 0 ? uploadResult.errors.join(', ') : 'None'}</span>
            </div>

            <div className="result-item">
              <span className="label">Timestamp:</span>
              <span className="value">
                {new Date().toLocaleString('en-ZA')}
              </span>
            </div>
          </div>
        </div>
      )}

      <div className="upload-info">
        <h4>Supported Document Types</h4>
        <div className="document-types">
          <div className="doc-type">
            <FileText size={20} />
            <div>
              <strong>Bank Statements</strong>
              <p>PDF or CSV format bank transaction statements</p>
            </div>
          </div>

          <div className="doc-type">
            <FileText size={20} />
            <div>
              <strong>Invoices & Receipts</strong>
              <p>Digital invoices, receipts, and payment confirmations</p>
            </div>
          </div>

          <div className="doc-type">
            <FileText size={20} />
            <div>
              <strong>Financial Reports</strong>
              <p>Balance sheets, income statements, and other reports</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}