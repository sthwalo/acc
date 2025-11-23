import React from 'react';

interface ApiMessageBannerProps {
  message: string | null;
  type: 'success' | 'error' | 'info';
}

const ApiMessageBanner: React.FC<ApiMessageBannerProps> = ({ message, type }) => {
  if (!message) return null;

  const getClassName = () => {
    switch (type) {
      case 'success': return 'bg-green-100 border-green-500 text-green-700';
      case 'error': return 'bg-red-100 border-red-500 text-red-700';
      case 'info': return 'bg-blue-100 border-blue-500 text-blue-700';
      default: return 'bg-gray-100 border-gray-500 text-gray-700';
    }
  };

  return (
    <div className={`border-l-4 p-4 mb-4 ${getClassName()}`}>
      <p>{message}</p>
    </div>
  );
};

export default ApiMessageBanner;