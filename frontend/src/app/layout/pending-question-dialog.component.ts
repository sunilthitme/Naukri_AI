import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PendingQuestion } from '../core/models';

@Component({
  selector: 'app-pending-question-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>question_answer</mat-icon>
      Answer required
    </h2>
    <mat-dialog-content>
      <p class="question">{{ data.question }}</p>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Your answer</mat-label>
          <textarea matInput rows="4" formControlName="answer"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button color="primary" (click)="submit()" [disabled]="form.invalid">
        <mat-icon>send</mat-icon>
        Save Answer & Continue
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .question {
      margin: 0 0 16px;
      color: #172033;
      line-height: 1.5;
      white-space: pre-wrap;
    }
    mat-dialog-content {
      min-width: min(520px, 82vw);
    }
  `]
})
export class PendingQuestionDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PendingQuestionDialogComponent, string>);
  readonly data = inject<PendingQuestion>(MAT_DIALOG_DATA);
  readonly form = this.fb.nonNullable.group({
    answer: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.dialogRef.close(this.form.controls.answer.value.trim());
  }
}
