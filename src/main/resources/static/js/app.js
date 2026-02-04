document.addEventListener('DOMContentLoaded', () => {
    const queryInput = document.getElementById('queryInput');
    const submitBtn = document.getElementById('submitBtn');
    const resultSection = document.getElementById('resultSection');
    const resultsTable = document.getElementById('resultsTable');
    const sqlOutput = document.getElementById('sqlOutput');
    const loadingIndicator = document.getElementById('loadingIndicator');
    const errorBanner = document.getElementById('errorBanner');
    const errorMessage = document.getElementById('errorMessage');

    // Stats elements
    const executionTimeMs = document.getElementById('executionTime');
    const rowCount = document.getElementById('rowCount');
    const confidenceScore = document.getElementById('confidenceScore');

    // Auto-resize textarea
    queryInput.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });

    // Submit on Enter (Shift+Enter for new line)
    queryInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submitQuery();
        }
    });

    submitBtn.addEventListener('click', submitQuery);

    // Suggestion chips
    document.querySelectorAll('.suggestion-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            queryInput.value = chip.textContent;
            queryInput.focus();
            submitQuery();
        });
    });

    async function submitQuery() {
        const query = queryInput.value.trim();
        if (!query) return;

        // Reset UI
        resultSection.classList.add('hidden');
        errorBanner.classList.add('hidden');
        loadingIndicator.classList.remove('hidden');

        try {
            const response = await fetch('/api/query', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Basic ' + btoa('admin:admin') // Basic Auth
                },
                body: JSON.stringify({ query: query })
            });

            const data = await response.json();

            loadingIndicator.classList.add('hidden');

            if (data.errorMessage) {
                showError(data.errorMessage);
                return;
            }

            renderResults(data);

        } catch (error) {
            loadingIndicator.classList.add('hidden');
            showError("Failed to connect to the server. Is it running?");
            console.error(error);
        }
    }

    function renderResults(data) {
        // Update Metadata
        sqlOutput.textContent = data.generatedQuery || "N/A";
        executionTimeMs.textContent = `${data.executionTimeMs}ms`;
        rowCount.textContent = data.rowCount;
        confidenceScore.textContent = `${Math.round((data.confidence || 0) * 100)}%`;

        const tableHead = resultsTable.querySelector('thead');
        const tableBody = resultsTable.querySelector('tbody');

        tableHead.innerHTML = '';
        tableBody.innerHTML = '';

        if (data.results && data.results.length > 0) {
            // Get columns from first row
            const columns = Object.keys(data.results[0]);

            // Create Header
            const headerRow = document.createElement('tr');
            columns.forEach(col => {
                const th = document.createElement('th');
                th.textContent = formatColumnName(col);
                headerRow.appendChild(th);
            });
            tableHead.appendChild(headerRow);

            // Create Rows
            data.results.forEach(row => {
                const tr = document.createElement('tr');
                columns.forEach(col => {
                    const td = document.createElement('td');
                    td.textContent = row[col];
                    tr.appendChild(td);
                });
                tableBody.appendChild(tr);
            });
        } else {
            // Empty state
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.textContent = "No results found.";
            td.colSpan = 100;
            td.style.textAlign = 'center';
            td.style.color = 'var(--text-muted)';
            tr.appendChild(td);
            tableBody.appendChild(tr);
        }

        resultSection.classList.remove('hidden');
    }

    function showError(msg) {
        errorMessage.textContent = msg;
        errorBanner.classList.remove('hidden');
    }

    function formatColumnName(name) {
        // Convert snake_case to Title Case
        return name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
    }
});
