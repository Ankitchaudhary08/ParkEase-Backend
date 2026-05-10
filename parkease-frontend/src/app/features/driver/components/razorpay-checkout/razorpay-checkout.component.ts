import {
  Component, Input, Output, EventEmitter,
  OnInit, OnDestroy, signal, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentService, Payment, RazorpayOrderResponse } from '../../../../core/services/payment.service';
import { AuthService } from '../../../../core/services/auth.service';

declare global {
  interface Window {
    Razorpay: new (options: RazorpayOptions) => RazorpayInstance;
  }
}

interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  prefill: { name: string; email: string };
  theme: { color: string };
  modal: { ondismiss: () => void };
  handler: (response: RazorpayPaymentResponse) => void;
}

interface RazorpayInstance {
  open(): void;
}

interface RazorpayPaymentResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

@Component({
  selector: 'app-razorpay-checkout',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './razorpay-checkout.component.html',
  styleUrl: './razorpay-checkout.component.css'
})
export class RazorpayCheckoutComponent implements OnInit, OnDestroy {
  @Input({ required: true }) bookingId!: number;
  @Input({ required: true }) amount!: number;
  @Input() description = 'ParkEase Parking Payment';

  @Output() paymentSuccess = new EventEmitter<Payment>();
  @Output() paymentError = new EventEmitter<string>();
  @Output() paymentDismissed = new EventEmitter<void>();

  private paymentService = inject(PaymentService);
  private authService = inject(AuthService);

  loading = signal(false);
  errorMessage = signal<string | null>(null);
  private scriptLoaded = false;

  ngOnInit(): void {
    this.loadRazorpayScript();
  }

  ngOnDestroy(): void {
    const script = document.getElementById('razorpay-script');
    if (script) script.remove();
  }

  pay(): void {
    if (!this.scriptLoaded) {
      this.errorMessage.set('Payment gateway not loaded. Please refresh and try again.');
      return;
    }
    const user = this.authService.currentUser();
    if (!user) {
      this.errorMessage.set('Please log in to make a payment.');
      return;
    }
    this.errorMessage.set(null);
    this.loading.set(true);

    this.paymentService.createRazorpayOrder({
      bookingId: this.bookingId,
      userId: user.userId,
      amount: this.amount,
      description: this.description
    }).subscribe({
      next: (order) => this.openWidget(order, user),
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? 'Failed to initiate payment. Please try again.';
        this.errorMessage.set(msg);
        this.paymentError.emit(msg);
      }
    });
  }

  private openWidget(order: RazorpayOrderResponse, user: { fullName: string; email: string; userId: number }): void {
    const options: RazorpayOptions = {
      key: order.keyId,
      amount: order.amountInPaise,
      currency: order.currency,
      name: 'ParkEase',
      description: this.description,
      order_id: order.razorpayOrderId,
      prefill: { name: user.fullName, email: user.email },
      theme: { color: '#1976d2' },
      modal: {
        ondismiss: () => {
          this.loading.set(false);
          this.paymentDismissed.emit();
        }
      },
      handler: (response: RazorpayPaymentResponse) => {
        this.capturePayment(response, order, user.userId);
      }
    };

    const rzp = new window.Razorpay(options);
    this.loading.set(false);
    rzp.open();
  }

  private capturePayment(
    response: RazorpayPaymentResponse,
    order: RazorpayOrderResponse,
    userId: number
  ): void {
    this.loading.set(true);
    this.paymentService.verifyRazorpayPayment({
      bookingId: this.bookingId,
      userId,
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature,
      amount: this.amount
    }).subscribe({
      next: (payment) => {
        this.loading.set(false);
        this.paymentSuccess.emit(payment);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? 'Payment verification failed. Contact support with your payment ID.';
        this.errorMessage.set(msg);
        this.paymentError.emit(msg);
      }
    });
  }

  private loadRazorpayScript(): void {
    if (document.getElementById('razorpay-script')) {
      this.scriptLoaded = true;
      return;
    }
    const script = document.createElement('script');
    script.id = 'razorpay-script';
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => { this.scriptLoaded = true; };
    script.onerror = () => {
      this.errorMessage.set('Failed to load payment gateway. Check your internet connection.');
    };
    document.body.appendChild(script);
  }
}
