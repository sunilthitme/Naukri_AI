import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { QuestionItem } from '../../core/models/app.models';

@Component({
  selector: 'app-questions-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './questions-page.component.html',
  styleUrl: './questions-page.component.css'
})
export class QuestionsPageComponent implements OnChanges {
  @Input() questions: QuestionItem[] = [];
  @Input() busy = false;
  @Input() suggestingQuestionId: number | null = null;
  @Input() suggestedAnswers: Record<number, string> = {};
  @Output() answer = new EventEmitter<{ questionId: number; answer: string }>();
  @Output() suggest = new EventEmitter<number>();

  draftAnswers: Record<number, string> = {};

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['suggestedAnswers']) {
      for (const [questionId, answer] of Object.entries(this.suggestedAnswers)) {
        this.draftAnswers[Number(questionId)] = answer;
      }
    }
  }

  submit(question: QuestionItem): void {
    const answer = (this.draftAnswers[question.id] || '').trim();
    if (answer) {
      this.answer.emit({ questionId: question.id, answer });
    }
  }

  askAi(questionId: number): void {
    this.suggest.emit(questionId);
  }
}
