import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ApiService } from '../../core/api.service';
import { JobFilterResponse } from '../../core/models';

@Component({
  selector: 'app-filters',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSnackBarModule,
    MatSlideToggleModule
  ],
  template: `
    <section class="page">
      <div class="page-header">
        <h1 class="page-title">Job Filter Configuration</h1>
      </div>
      <mat-card>
        <mat-card-content>
          <form class="grid cols-2" [formGroup]="form" (ngSubmit)="save()">
            <mat-form-field appearance="outline">
              <mat-label>Keywords</mat-label>
              <input matInput formControlName="keywords">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Experience</mat-label>
              <input matInput formControlName="experience">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Location</mat-label>
              <input matInput formControlName="location">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Salary</mat-label>
              <input matInput formControlName="salary">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Work mode</mat-label>
              <input matInput formControlName="workMode">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Freshness</mat-label>
              <input matInput formControlName="freshness">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Preferred companies</mat-label>
              <textarea matInput rows="3" formControlName="preferredCompanies"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Blacklisted companies</mat-label>
              <textarea matInput rows="3" formControlName="blacklistedCompanies"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Daily apply limit</mat-label>
              <input matInput type="number" formControlName="dailyApplyLimit">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Duplicate prevention days</mat-label>
              <input matInput type="number" formControlName="duplicatePreventionDays">
            </mat-form-field>
            <div class="toggle-grid">
              <mat-slide-toggle formControlName="autoApply">Auto apply</mat-slide-toggle>
              <mat-slide-toggle formControlName="externalCareerApply">External career apply</mat-slide-toggle>
              <mat-slide-toggle formControlName="easyApplyOnly">Easy apply only</mat-slide-toggle>
            </div>
            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
                <mat-icon>save</mat-icon>
                Save filters
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </section>
  `,
  styles: [`
    .toggle-grid {
      display: grid;
      align-content: start;
      gap: 14px;
      padding: 8px 0;
    }
  `]
})
export class FiltersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);

  readonly form = this.fb.nonNullable.group({
    keywords: [''],
    experience: [''],
    location: [''],
    salary: [''],
    workMode: [''],
    freshness: [''],
    preferredCompanies: [''],
    blacklistedCompanies: [''],
    autoApply: [true],
    externalCareerApply: [false],
    dailyApplyLimit: [20, [Validators.min(1), Validators.max(500)]],
    easyApplyOnly: [true],
    duplicatePreventionDays: [30, [Validators.min(1), Validators.max(365)]]
  });

  ngOnInit(): void {
    this.api.filters().subscribe((response) => {
      this.form.patchValue({
        ...response,
        preferredCompanies: response.preferredCompanies.join('\n'),
        blacklistedCompanies: response.blacklistedCompanies.join('\n')
      });
    });
  }

  save(): void {
    const raw = this.form.getRawValue();
    const payload: JobFilterResponse = {
      ...raw,
      preferredCompanies: this.lines(raw.preferredCompanies),
      blacklistedCompanies: this.lines(raw.blacklistedCompanies),
      updatedAt: null
    };
    this.api.saveFilters(payload).subscribe({
      next: () => this.snack.open('Filters saved', 'Close', { duration: 3000 }),
      error: () => this.snack.open('Unable to save filters', 'Close', { duration: 3000 })
    });
  }

  private lines(value: string): string[] {
    return value.split(/\r?\n|,/).map((item) => item.trim()).filter(Boolean);
  }
}
