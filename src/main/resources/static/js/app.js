document.addEventListener('DOMContentLoaded', () => {
    const queryInput = document.getElementById('queryInput');
    const submitBtn = document.getElementById('submitBtn');
    const resultSection = document.getElementById('resultSection');
    const resultsTable = document.getElementById('resultsTable');
    const sqlOutput = document.getElementById('sqlOutput');
    const loadingIndicator = document.getElementById('loadingIndicator');
    const errorBanner = document.getElementById('errorBanner');
    const errorMessage = document.getElementById('errorMessage');
    const dbSelect = document.getElementById('dbSelect');

    const refreshSchemaBtn = document.getElementById('refreshSchemaBtn');
    const schemaList = document.getElementById('schemaList');

    // Stats elements
    const executionTimeMs = document.getElementById('executionTime');
    const rowCount = document.getElementById('rowCount');
    const confidenceScore = document.getElementById('confidenceScore');

    // Load available databases
    loadDatabases();

    // Event Listeners
    refreshSchemaBtn.addEventListener('click', () => {
        loadSchema(dbSelect.value);
    });

    dbSelect.addEventListener('change', () => {
        loadSchema(dbSelect.value);
    });

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

    async function loadDatabases() {
        try {
            const response = await fetch('/api/query/databases', {
                headers: {
                    'Authorization': 'Basic ' + btoa('admin:admin')
                }
            });
            const databases = await response.json();

            dbSelect.innerHTML = '';
            databases.forEach(db => {
                const option = document.createElement('option');
                option.value = db;
                option.textContent = db;
                if (db === 'engdb') option.selected = true;
                dbSelect.appendChild(option);
            });

            // Initial schema load
            loadSchema(dbSelect.value);

        } catch (error) {
            console.error('Failed to load databases', error);
        }
    }

    async function loadSchema(dbName) {
        if (!dbName) return;

        schemaList.innerHTML = '<div class="loading"><div class="spinner" style="width:1.5rem;height:1.5rem;"></div></div>';

        try {
            const response = await fetch(`/api/query/schema?dbName=${dbName}`, {
                headers: {
                    'Authorization': 'Basic ' + btoa('admin:admin')
                }
            });
            const schema = await response.json();
            renderSchema(schema);
        } catch (error) {
            schemaList.innerHTML = '<div class="error-msg">Failed to load schema</div>';
            console.error('Failed to load schema', error);
        }
    }

    function renderSchema(schema) {
        schemaList.innerHTML = '';
        const tables = Object.keys(schema);

        if (tables.length === 0) {
            schemaList.innerHTML = '<div class="schema-placeholder">No tables found</div>';
            return;
        }

        tables.forEach(tableName => {
            const columns = schema[tableName];

            const tableItem = document.createElement('div');
            tableItem.className = 'table-item';

            // Header
            const header = document.createElement('div');
            header.className = 'table-header';
            header.innerHTML = `
                <span class="table-icon">🗃️</span>
                <span>${tableName}</span>
            `;

            // Columns Container
            const columnsContainer = document.createElement('div');
            columnsContainer.className = 'table-columns';

            columns.forEach(col => {
                const colItem = document.createElement('div');
                colItem.className = 'column-item';
                colItem.innerHTML = `
                    <span class="column-icon">🔹</span>
                    <span>${col}</span>
                `;
                columnsContainer.appendChild(colItem);
            });

            // Toggle Expand
            header.addEventListener('click', () => {
                columnsContainer.classList.toggle('expanded');
            });

            tableItem.appendChild(header);
            tableItem.appendChild(columnsContainer);
            schemaList.appendChild(tableItem);
        });
    }

    async function submitQuery() {
        const query = queryInput.value.trim();
        const dbName = dbSelect.value;
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
                body: JSON.stringify({
                    query: query,
                    databaseName: dbName
                })
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
