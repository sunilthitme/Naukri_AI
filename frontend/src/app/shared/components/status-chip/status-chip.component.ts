import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-chip.component.html',
  styleUrl: './status-chip.component.css'
})
export class StatusChipComponent {
  @Input() status = 'IDLE';
}
