/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

import React, { useEffect, useRef, useState } from 'react';
import { loadScript } from '@paypal/paypal-js';
import type { OnApproveData, PayPalScriptOptions } from '@paypal/paypal-js';
import { serviceRegistry } from '../../services/ServiceRegistry';
import { ApiService } from '../../services/ApiService';
import type { PayPalCreateOrderRequest, PayPalCaptureOrderRequest, PayPalError } from '../../types/api';

interface PayPalButtonProps {
  amount: number;
  currency?: string;
  description?: string;
  planId?: number;
  onSuccess: (orderId: string, captureId?: string) => void;
  onError: (error: string) => void;
  disabled?: boolean;
  className?: string;
}

export const PayPalButton: React.FC<PayPalButtonProps> = ({
  amount,
  currency = 'USD',
  description,
  planId,
  onSuccess,
  onError,
  disabled = false,
  className = ''
}) => {
  const paypalRef = useRef<HTMLDivElement>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [paypalLoaded, setPaypalLoaded] = useState(false);

  useEffect(() => {
    const loadPayPalScript = async () => {
      try {
        // Check for dummy mode
        const dummyMode = import.meta.env.VITE_PAYPAL_DUMMY_MODE === 'true';
        console.log('PayPal Dummy Mode:', dummyMode);

        if (dummyMode) {
          console.log('DUMMY MODE: Skipping PayPal script load');
          setPaypalLoaded(true);
          return;
        }

        // Load PayPal script with client ID from environment
        const clientId = import.meta.env.VITE_PAYPAL_CLIENT_ID;
        console.log('PayPal Client ID:', clientId ? 'Set' : 'Not set');
        console.log('PayPal Client ID value:', clientId);

        const loadOptions: PayPalScriptOptions = {
          clientId: clientId || 'test',
          currency: currency,
          enableFunding: 'paypal',
          disableFunding: 'paylater,card'
        };

        const paypal = await loadScript(loadOptions);

        if (paypal && typeof paypal.Buttons === 'function' && paypalRef.current) {
          paypal.Buttons({
            createOrder: async () => {
              try {
                const request: PayPalCreateOrderRequest = {
                  amount,
                  currency,
                  description,
                  planId
                };

                const apiService = serviceRegistry.get<ApiService>('apiService');
                const response = await apiService.paypal.createOrder(request);
                return response.orderId;
              } catch (error) {
                console.error('Error creating PayPal order:', error);
                const errorMessage = error instanceof Error ? error.message : 'Failed to create payment order. Please try again.';
                onError(errorMessage);
                throw error;
              }
            },

            onApprove: async (data: OnApproveData) => {
              try {
                const request: PayPalCaptureOrderRequest = {
                  orderId: data.orderID,
                  planId
                };

                const apiService = serviceRegistry.get<ApiService>('apiService');
                const response = await apiService.paypal.captureOrder(request);

                if (response.completed) {
                  onSuccess(data.orderID, response.captureId);
                } else {
                  onError('Payment was not completed successfully.');
                }
              } catch (error) {
                console.error('Error capturing PayPal order:', error);
                const errorMessage = error instanceof Error ? error.message : 'Failed to process payment. Please contact support if the charge appears on your statement.';
                onError(errorMessage);
              }
            },

            onError: (error: PayPalError | Error | unknown) => {
              console.error('PayPal button error:', error);
              let errorMessage = 'Payment failed. Please try again.';

              if (error instanceof Error) {
                errorMessage = error.message;
              } else if (error && typeof error === 'object' && 'message' in error && typeof error.message === 'string') {
                errorMessage = error.message;
              }

              onError(errorMessage);
            },

            style: {
              layout: 'vertical',
              color: 'blue',
              shape: 'rect',
              label: 'paypal'
            }
          }).render(paypalRef.current);

          setPaypalLoaded(true);
        }
      } catch (error) {
        console.error('Failed to load PayPal script:', error);
        const errorMessage = error instanceof Error ? error.message : 'Failed to load payment system. Please refresh the page.';
        onError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    loadPayPalScript();
  }, [amount, currency, description, planId, onSuccess, onError]);

  const handleDummyPayment = async () => {
    try {
      // Create dummy order
      const request: PayPalCreateOrderRequest = {
        amount,
        currency,
        description,
        planId
      };

      const apiService = serviceRegistry.get<ApiService>('apiService');
      const orderResponse = await apiService.paypal.createOrder(request);

      // Simulate approval delay
      setTimeout(async () => {
        try {
          // Capture dummy order
          const captureRequest: PayPalCaptureOrderRequest = {
            orderId: orderResponse.orderId,
            planId
          };

          const captureResponse = await apiService.paypal.captureOrder(captureRequest);

          if (captureResponse.completed) {
            onSuccess(orderResponse.orderId, captureResponse.captureId);
          } else {
            onError('Dummy payment was not completed successfully.');
          }
        } catch (error) {
          console.error('Error capturing dummy order:', error);
          const errorMessage = error instanceof Error ? error.message : 'Failed to process dummy payment.';
          onError(errorMessage);
        }
      }, 1000); // 1 second delay to simulate PayPal approval

    } catch (error) {
      console.error('Error creating dummy order:', error);
      const errorMessage = error instanceof Error ? error.message : 'Failed to create dummy payment order.';
      onError(errorMessage);
    }
  };

  if (disabled) {
    return (
      <div className={`paypal-button-container ${className}`}>
        <div className="bg-gray-100 border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
          <div className="text-gray-500 mb-2">
            <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <p className="text-sm text-gray-600">Complete the form above to enable payment</p>
        </div>
      </div>
    );
  }

  const dummyMode = import.meta.env.VITE_PAYPAL_DUMMY_MODE === 'true';

  return (
    <div className={`paypal-button-container ${className}`}>
      {isLoading && (
        <div className="flex items-center justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-2 text-gray-600">Loading payment options...</span>
        </div>
      )}

      {!isLoading && !paypalLoaded && !dummyMode && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center">
          <p className="text-red-700 text-sm">Failed to load PayPal. Please refresh the page.</p>
        </div>
      )}

      {dummyMode && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6 text-center">
          <div className="mb-4">
            <svg className="mx-auto h-12 w-12 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
          <h3 className="text-lg font-medium text-yellow-800 mb-2">Dummy Payment Mode</h3>
          <p className="text-yellow-700 text-sm mb-4">
            This is a test environment. No real payment will be processed.
          </p>
          <button
            onClick={handleDummyPayment}
            disabled={isLoading}
            className="bg-yellow-600 hover:bg-yellow-700 disabled:bg-yellow-400 text-white font-medium py-3 px-6 rounded-lg transition-colors duration-200 flex items-center justify-center mx-auto"
          >
            {isLoading ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                Processing...
              </>
            ) : (
              <>
                <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
                Complete Dummy Payment
              </>
            )}
          </button>
        </div>
      )}

      <div ref={paypalRef} className={isLoading || dummyMode ? 'hidden' : ''}></div>
    </div>
  );
};