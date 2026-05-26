import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-credentials',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSnackBarModule],
  template: `
    <section class="page">
      <div class="page-header">
        <h1 class="page-title">Naukri Credentials</h1>
      </div>
      <mat-card>
        <mat-card-content>
          <form class="grid cols-2" [formGroup]="form" (ngSubmit)="save()">
            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="off">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="new-password">
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Default resume path</mat-label>
              <input matInput formControlName="resumePath" placeholder="C:\\Users\\Admin\\Documents\\resume.pdf">
            </mat-form-field>
            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
                <mat-icon>save</mat-icon>
                Save
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </section>
  `
})
export class CredentialsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    resumePath: ['']
  });

  ngOnInit(): void {
    this.api.credentials().subscribe((response) => {
      this.form.patchValue({ email: response.email ?? '', resumePath: response.resumePath ?? '' });
    });
  }

  save(): void {
    this.api.saveCredentials(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.controls.password.reset('');
        this.snack.open('Credentials saved with AES encryption', 'Close', { duration: 3000 });
      },
      error: () => this.snack.open('Unable to save credentials', 'Close', { duration: 3000 })
    });
  }
}
