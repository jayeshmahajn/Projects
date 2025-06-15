
class DocumentUploader {
    constructor() {
        this.initializeElements();
        this.attachEventListeners();
        this.maxFileSize = 50 * 1024 * 1024; // 50MB
        this.allowedTypes = [
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-powerpoint',
            'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            'application/vnd.oasis.opendocument.text',
            'application/vnd.oasis.opendocument.spreadsheet',
            'application/vnd.oasis.opendocument.presentation',
            'application/rtf',
            'text/rtf'
        ];
    }

    initializeElements() {
        this.dropZone = document.getElementById('dropZone');
        this.fileInput = document.getElementById('fileInput');
        this.fileInfo = document.getElementById('fileInfo');
        this.uploadForm = document.getElementById('uploadForm');
        this.convertBtn = document.getElementById('convertBtn');
        this.removeFileBtn = document.getElementById('removeFile');
        this.progressSection = document.getElementById('progressSection');
        this.progressFill = document.getElementById('progressFill');
        this.progressText = document.getElementById('progressText');
    }

    attachEventListeners() {
        // Drag and drop events
        this.dropZone.addEventListener('dragover', this.handleDragOver.bind(this));
        this.dropZone.addEventListener('dragleave', this.handleDragLeave.bind(this));
        this.dropZone.addEventListener('drop', this.handleDrop.bind(this));
        this.dropZone.addEventListener('click', () => this.fileInput.click());

        // File input change
        this.fileInput.addEventListener('change', this.handleFileSelect.bind(this));

        // Form submission
        this.uploadForm.addEventListener('submit', this.handleFormSubmit.bind(this));

        // Remove file button
        this.removeFileBtn.addEventListener('click', this.removeFile.bind(this));

        // Prevent default drag behaviors on document
        document.addEventListener('dragover', e => e.preventDefault());
        document.addEventListener('drop', e => e.preventDefault());
    }

    handleDragOver(e) {
        e.preventDefault();
        this.dropZone.classList.add('dragover');
    }

    handleDragLeave(e) {
        e.preventDefault();
        if (!this.dropZone.contains(e.relatedTarget)) {
            this.dropZone.classList.remove('dragover');
        }
    }

    handleDrop(e) {
        e.preventDefault();
        this.dropZone.classList.remove('dragover');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            this.processFile(files[0]);
        }
    }

    handleFileSelect(e) {
        const file = e.target.files[0];
        if (file) {
            this.processFile(file);
        }
    }

    processFile(file) {
        if (!this.validateFile(file)) {
            return;
        }

        this.displayFileInfo(file);
        this.convertBtn.disabled = false;
    }

    validateFile(file) {
        // Check file size
        if (file.size > this.maxFileSize) {
            this.showError('File size must be less than 50MB');
            return false;
        }

        // Check file type
        const fileExtension = this.getFileExtension(file.name).toLowerCase();
        const allowedExtensions = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'odt', 'ods', 'odp', 'rtf'];
        
        if (!allowedExtensions.includes(fileExtension)) {
            this.showError('Unsupported file format. Please select a valid document.');
            return false;
        }

        return true;
    }

    displayFileInfo(file) {
        const fileName = this.fileInfo.querySelector('.file-name');
        const fileSize = this.fileInfo.querySelector('.file-size');
        
        fileName.textContent = file.name;
        fileSize.textContent = this.formatFileSize(file.size);
        
        this.fileInfo.style.display = 'flex';
        this.dropZone.style.display = 'none';
    }

    removeFile() {
        this.fileInput.value = '';
        this.fileInfo.style.display = 'none';
        this.dropZone.style.display = 'block';
        this.convertBtn.disabled = true;
    }

    async handleFormSubmit(e) {
        e.preventDefault();
        
        const formData = new FormData(this.uploadForm);
        
        if (!formData.get('file') || formData.get('file').size === 0) {
            this.showError('Please select a file to convert');
            return;
        }

        // Log form data for debugging
        console.log('Form data:', {
            file: formData.get('file'),
            fileName: formData.get('file').name,
            fileSize: formData.get('file').size,
            customFilename: formData.get('filename')
        });

        this.showProgress();
        
        try {
            const response = await fetch('/convert', {
                method: 'POST',
                body: formData
            });

            console.log('Response status:', response.status);
            console.log('Response headers:', Object.fromEntries(response.headers.entries()));

            if (response.ok) {
                const blob = await response.blob();
                const filename = this.getDownloadFilename(response);
                this.downloadFile(blob, filename);
                this.showSuccess('File converted successfully!');
            } else {
                // Try to get error message from response
                let errorMessage = 'Conversion failed';
                try {
                    const contentType = response.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        const errorData = await response.json();
                        errorMessage = errorData.error || errorMessage;
                    } else {
                        const errorText = await response.text();
                        if (errorText) {
                            errorMessage = errorText;
                        }
                    }
                } catch (parseError) {
                    console.error('Error parsing error response:', parseError);
                }
                
                console.error('Server error:', errorMessage);
                this.showError(errorMessage);
            }
        } catch (error) {
            console.error('Network error:', error);
            this.showError('Network error: Unable to connect to server. Please check if the server is running.');
        } finally {
            this.hideProgress();
        }
    }

    showProgress() {
        this.convertBtn.disabled = true;
        this.convertBtn.classList.add('loading');
        this.progressSection.style.display = 'block';
        
        // Animate progress bar
        let progress = 0;
        const interval = setInterval(() => {
            progress += Math.random() * 15;
            if (progress > 90) progress = 90;
            this.progressFill.style.width = progress + '%';
        }, 200);
        
        this.progressInterval = interval;
    }

    hideProgress() {
        clearInterval(this.progressInterval);
        this.progressFill.style.width = '100%';
        
        setTimeout(() => {
            this.progressSection.style.display = 'none';
            this.progressFill.style.width = '0%';
            this.convertBtn.disabled = false;
            this.convertBtn.classList.remove('loading');
        }, 1000);
    }

    downloadFile(blob, filename) {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }

    getDownloadFilename(response) {
        const disposition = response.headers.get('Content-Disposition');
        if (disposition && disposition.includes('filename=')) {
            const matches = disposition.match(/filename="([^"]+)"/);
            if (matches) return matches[1];
        }
        return 'converted.pdf';
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    getFileExtension(filename) {
        return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2);
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showNotification(message, type) {
        // Remove existing notifications
        const existing = document.querySelector('.notification');
        if (existing) existing.remove();

        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <i class="fas fa-${type === 'error' ? 'exclamation-circle' : 'check-circle'}"></i>
            <span>${message}</span>
            <button class="close-notification">
                <i class="fas fa-times"></i>
            </button>
        `;

        // Add styles for notification
        const style = document.createElement('style');
        style.textContent = `
            .notification {
                position: fixed;
                top: 20px;
                right: 20px;
                padding: 15px 20px;
                border-radius: 8px;
                color: white;
                font-weight: 500;
                z-index: 1000;
                display: flex;
                align-items: center;
                gap: 10px;
                min-width: 300px;
                animation: slideIn 0.3s ease;
            }
            .notification.error {
                background: var(--danger-color);
            }
            .notification.success {
                background: var(--success-color);
            }
            .close-notification {
                background: none;
                border: none;
                color: white;
                cursor: pointer;
                margin-left: auto;
                padding: 5px;
            }
            @keyframes slideIn {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
        `;
        
        if (!document.querySelector('style[data-notification]')) {
            style.setAttribute('data-notification', '');
            document.head.appendChild(style);
        }

        document.body.appendChild(notification);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);

        // Close button functionality
        notification.querySelector('.close-notification').addEventListener('click', () => {
            notification.remove();
        });
    }
}

// Initialize the uploader when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new DocumentUploader();
});