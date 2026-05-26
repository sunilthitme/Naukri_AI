import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ApiService } from '../../core/api.service';
import { QuestionAnswer } from '../../core/models';

@Component({
  selector: 'app-questions',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSnackBarModule,
    MatTableModule
  ],
  template: `
    <section class="page">
      <div class="page-header">
        <h1 class="page-title">Question & Answer Management</h1>
      </div>
      <mat-card>
        <mat-card-content>
          <form class="grid cols-2" [formGroup]="form" (ngSubmit)="save()">
            <mat-form-field appearance="outline">
              <mat-label>Question</mat-label>
              <textarea matInput rows="3" formControlName="question"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Answer</mat-label>
              <textarea matInput rows="3" formControlName="answer"></textarea>
            </mat-form-field>
            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
                <mat-icon>save</mat-icon>
                Save answer
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
      <div class="table-wrap">
        <table mat-table [dataSource]="questions()">
          <ng-container matColumnDef="question">
            <th mat-header-cell *matHeaderCellDef>Question</th>
            <td mat-cell *matCellDef="let row">{{ row.question }}</td>
          </ng-container>
          <ng-container matColumnDef="answer">
            <th mat-header-cell *matHeaderCellDef>Answer</th>
            <td mat-cell *matCellDef="let row">{{ row.answer }}</td>
          </ng-container>
          <ng-container matColumnDef="lastUsedAt">
            <th mat-header-cell *matHeaderCellDef>Last used</th>
            <td mat-cell *matCellDef="let row">{{ row.lastUsedAt ? (row.lastUsedAt | date:'short') : '-' }}</td>
          </ng-container>
          <ng-container matColumnDef="edit">
            <th mat-header-cell *matHeaderCellDef>Edit</th>
            <td mat-cell *matCellDef="let row">
              <button mat-icon-button aria-label="Edit answer" (click)="edit(row)">
                <mat-icon>edit</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </div>
    </section>
  `
})
export class QuestionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);
  readonly columns = ['question', 'answer', 'lastUsedAt', 'edit'];
  readonly questions = signal<QuestionAnswer[]>([]);
  readonly form = this.fb.nonNullable.group({
    question: ['', Validators.required],
    answer: ['', Validators.required]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.questions().subscribe((response) => this.questions.set(response));
  }

  edit(row: QuestionAnswer): void {
    this.form.patchValue({ question: row.question, answer: row.answer });
  }

  save(): void {
    this.api.saveQuestion(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.reset();
        this.snack.open('Answer saved', 'Close', { duration: 2500 });
        this.load();
      },
      error: () => this.snack.open('Unable to save answer', 'Close', { duration: 3000 })
    });
  }
}
