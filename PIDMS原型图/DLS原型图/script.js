// 侧边栏菜单交互
function initMenu() {
    const newMenuItems = document.querySelectorAll('.menu-item');
    newMenuItems.forEach(item => {
        const menuTitle = item.querySelector('.menu-title');
        const submenu = item.querySelector('.submenu');
        if (menuTitle && submenu) {
            submenu.style.display = '';
            if (!item.classList.contains('active')) {
                submenu.style.display = 'none';
            }
            menuTitle.addEventListener('click', (e) => {
                e.stopPropagation();
                
                newMenuItems.forEach(otherItem => {
                    const otherSubmenu = otherItem.querySelector('.submenu');
                    if (otherSubmenu) {
                        otherSubmenu.style.display = 'none';
                    }
                    otherItem.classList.remove('active');
                });
                
                item.classList.add('active');
                submenu.style.display = 'block';
            });
            
            const submenuLinks = submenu.querySelectorAll('a');
            submenuLinks.forEach(link => {
                link.addEventListener('click', (e) => {
                    e.stopPropagation();
                });
            });
        }
    });
}

// 表格交互
function initTable() {
    const checkboxes = document.querySelectorAll('table input[type="checkbox"]');
    const selectAllCheckbox = document.querySelector('table thead input[type="checkbox"]');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            checkboxes.forEach(checkbox => { checkbox.checked = this.checked; });
        });
    }
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() { updateSelectedCount(); });
    });
}

function updateSelectedCount() {
    const checkboxes = document.querySelectorAll('table input[type="checkbox"]:checked');
    const selectedCount = checkboxes.length;
    const countElement = document.querySelector('.page-info span:first-child');
    if (countElement) countElement.textContent = `已选择 ${selectedCount} 条`;
}

function initPagination() {
    const pageButtons = document.querySelectorAll('.page-btn');
    const pageInput = document.querySelector('.page-input');
    pageButtons.forEach(button => {
        button.addEventListener('click', function() {
            pageButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');
            if (this.textContent !== '‹' && this.textContent !== '›') {
                pageInput.value = this.textContent;
            }
        });
    });
    if (pageInput) {
        pageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const pageNumber = parseInt(this.value);
                if (!isNaN(pageNumber) && pageNumber > 0) console.log('跳转到第', pageNumber, '页');
            }
        });
    }
}

function initSearch() {
    const searchButton = document.querySelector('.search-box button');
    const searchInput = document.querySelector('.search-box input');
    if (searchButton && searchInput) {
        searchButton.addEventListener('click', function() {
            const searchTerm = searchInput.value.trim();
            if (searchTerm) { console.log('搜索:', searchTerm); }
        });
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const searchTerm = this.value.trim();
                if (searchTerm) { console.log('搜索:', searchTerm); }
            }
        });
    }
}

function initButtons() {
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(button => {
        button.addEventListener('click', function() {
            const buttonText = this.textContent.trim();
            console.log('点击按钮:', buttonText);
        });
    });
}

function initActionButtons() {
    const actionButtons = document.querySelectorAll('.action-btn');
    actionButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const action = this.textContent.trim();
            const row = this.closest('tr');
            const projectName = row ? row.querySelector('td:nth-child(2)').textContent : '';
            console.log(action, '项目:', projectName);
            if (action === '归档') { e.preventDefault(); if (confirm('确定要归档此项目吗？')) alert('已归档'); }
            if (action === '删除') { e.preventDefault(); if (confirm('确定要删除此项目吗？此操作不可恢复。')) alert('已删除'); }
        });
    });
}

function init() { initMenu(); initTable(); initPagination(); initSearch(); initButtons(); initActionButtons(); }

function toggleAdvancedSearch() {
    const advancedSearchDiv = document.getElementById('advancedSearch');
    if (advancedSearchDiv) advancedSearchDiv.style.display = advancedSearchDiv.style.display === 'none' ? 'block' : 'none';
}

function performAdvancedSearch() {
    console.log('执行高级搜索');
    const panel = document.getElementById('advancedSearch');
    if (!panel) return;
    
    const inputs = panel.querySelectorAll('input');
    const selects = panel.querySelectorAll('select');
    const filters = {};
    
    inputs.forEach((input, index) => {
        if (input.type !== 'checkbox' && input.value.trim()) {
            filters[index] = input.value.trim().toLowerCase();
        }
    });
    
    selects.forEach((select, index) => {
        if (select.value) {
            filters['select_' + index] = select.value;
        }
    });
    
    const tableBody = document.querySelector('.project-table tbody');
    if (!tableBody) return;
    
    const rows = tableBody.querySelectorAll('tr');
    rows.forEach(row => {
        let match = true;
        const cells = row.querySelectorAll('td');
        
        for (const key in filters) {
            if (key.startsWith('select_')) {
                const selectIndex = parseInt(key.replace('select_', ''));
                const selectValue = filters[key].toLowerCase();
                let cellIndex = getCellIndexForSelect(selectIndex, panel);
                if (cellIndex >= 0 && cellIndex < cells.length) {
                    if (!cells[cellIndex].textContent.toLowerCase().includes(selectValue)) {
                        match = false;
                        break;
                    }
                }
            } else {
                const inputIndex = parseInt(key);
                const searchValue = filters[key];
                let cellIndex = getCellIndexForInput(inputIndex, panel);
                if (cellIndex >= 0 && cellIndex < cells.length) {
                    if (!cells[cellIndex].textContent.toLowerCase().includes(searchValue)) {
                        match = false;
                        break;
                    }
                }
            }
        }
        
        row.style.display = match ? '' : 'none';
    });
}

function getCellIndexForInput(inputIndex, panel) {
    const fieldLabels = [];
    panel.querySelectorAll('.search-field label').forEach(label => {
        fieldLabels.push(label.textContent.trim());
    });
    
    const headerRow = document.querySelector('.project-table thead tr');
    if (!headerRow) return -1;
    
    const headers = headerRow.querySelectorAll('th');
    for (let i = 0; i < headers.length; i++) {
        const headerText = headers[i].textContent.trim();
        if (fieldLabels[inputIndex] && headerText.includes(fieldLabels[inputIndex])) {
            return i;
        }
    }
    return -1;
}

function getCellIndexForSelect(selectIndex, panel) {
    const fieldLabels = [];
    panel.querySelectorAll('.search-field label').forEach(label => {
        fieldLabels.push(label.textContent.trim());
    });
    
    const selectElements = panel.querySelectorAll('select');
    if (selectIndex >= selectElements.length) return -1;
    
    const label = selectElements[selectIndex].previousElementSibling;
    if (!label || label.tagName !== 'LABEL') return -1;
    
    const labelText = label.textContent.trim();
    const headerRow = document.querySelector('.project-table thead tr');
    if (!headerRow) return -1;
    
    const headers = headerRow.querySelectorAll('th');
    for (let i = 0; i < headers.length; i++) {
        const headerText = headers[i].textContent.trim();
        if (headerText.includes(labelText)) {
            return i;
        }
    }
    return -1;
}

function resetAdvancedSearch() {
    const advancedSearchDiv = document.getElementById('advancedSearch');
    if (advancedSearchDiv) {
        advancedSearchDiv.querySelectorAll('input').forEach(input => { input.value = ''; });
        advancedSearchDiv.querySelectorAll('select').forEach(select => { select.value = ''; });
    }
    
    const tableBody = document.querySelector('.project-table tbody');
    if (tableBody) {
        const rows = tableBody.querySelectorAll('tr');
        rows.forEach(row => { row.style.display = ''; });
    }
    
    console.log('重置高级搜索条件');
}

window.addEventListener('DOMContentLoaded', init);
