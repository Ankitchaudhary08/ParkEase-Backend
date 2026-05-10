import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService } from '../../../../core/services/booking.service';
import { Payment } from '../../../../core/services/payment.service';
import { RazorpayCheckoutComponent } from '../../components/razorpay-checkout/razorpay-checkout.component';

@Component({
  selector: 'app-booking-payment',
  standalone: true,
  imports: [CommonModule, RazorpayCheckoutComponent],
  templateUrl: './booking-payment.component.html',
  styleUrl: './booking-payment.component.css'
})
export class BookingPaymentComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private bookingService = inject(BookingService);

  bookingId = signal<number | null>(null);
  amount = signal<number>(0);
  loading = signal(true);
  paid = signal(false);
  paidPayment = signal<Payment | null>(null);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.bookingId.set(id);
    this.bookingService.calculateAmount(id).subscribe({
      next: (amt) => { this.amount.set(amt); this.loading.set(false); },
      error: () => { this.error.set('Could not load booking amount.'); this.loading.set(false); }
    });
  }

  onPaymentSuccess(payment: Payment): void {
    this.paid.set(true);
    this.paidPayment.set(payment);
  }

  onPaymentError(msg: string): void {
    this.error.set(msg);
  }

  onDismissed(): void {
    this.error.set('Payment was cancelled. You can try again.');
  }

  goToBookings(): void {
    this.router.navigate(['/driver/bookings']);
  }
}
