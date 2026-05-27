export function downloadBlob(blob: Blob, fileName: string, mimeType: string): void {
  const typedBlob = blob.type ? blob : new Blob([blob], { type: mimeType });
  const url = URL.createObjectURL(typedBlob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
