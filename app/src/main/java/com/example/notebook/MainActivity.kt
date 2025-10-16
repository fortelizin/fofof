package com.example.notebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private val notebookViewModel: NotebookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotebookScreen(notebookViewModel = notebookViewModel)
                }
            }
        }
    }
}

data class PageEntry(
    val title: String,
    val content: String
)

class NotebookViewModel : ViewModel() {
    private val _pages = mutableStateListOf<PageEntry>()
    var selectedIndex by mutableStateOf(0)
        private set
    var searchQuery by mutableStateOf("")
        private set

    val pages: List<PageEntry>
        get() = _pages

    init {
        if (_pages.isEmpty()) {
            _pages.add(PageEntry(title = "Страница 1", content = ""))
        }
    }

    fun updateSearch(query: String) {
        searchQuery = query
        val filtered = filteredPages()
        if (filtered.isNotEmpty() && filtered.none { it.first == selectedIndex }) {
            selectedIndex = filtered.first().first
        }
    }

    fun selectPage(index: Int) {
        if (index in _pages.indices) {
            selectedIndex = index
        }
    }

    fun addPage() {
        val nextNumber = _pages.size + 1
        _pages.add(PageEntry(title = "Страница $nextNumber", content = ""))
        selectedIndex = _pages.lastIndex
    }

    fun deleteCurrentPage() {
        if (_pages.size <= 1) return
        if (selectedIndex in _pages.indices) {
            _pages.removeAt(selectedIndex)
            selectedIndex = selectedIndex.coerceAtMost(_pages.lastIndex)
        }
    }

    fun updateTitle(newTitle: String) {
        if (selectedIndex in _pages.indices) {
            val current = _pages[selectedIndex]
            _pages[selectedIndex] = current.copy(title = newTitle)
        }
    }

    fun updateContent(newContent: String) {
        if (selectedIndex in _pages.indices) {
            val current = _pages[selectedIndex]
            _pages[selectedIndex] = current.copy(content = newContent)
        }
    }

    fun filteredPages(): List<Pair<Int, PageEntry>> {
        val query = searchQuery.trim().lowercase()
        return _pages.withIndex()
            .filter { (index, page) ->
                query.isEmpty() || page.title.lowercase().contains(query)
            }
            .map { it.index to it.value }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(notebookViewModel: NotebookViewModel = viewModel()) {
    val filteredPages = notebookViewModel.filteredPages()
    val selectedIndex = notebookViewModel.selectedIndex
    val searchQuery = notebookViewModel.searchQuery

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Записная книжка") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { notebookViewModel.addPage() }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить страницу")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = notebookViewModel::updateSearch,
                onAddPage = notebookViewModel::addPage,
                onDeletePage = notebookViewModel::deleteCurrentPage,
                deleteEnabled = notebookViewModel.pages.size > 1
            )

            PageList(
                pages = filteredPages,
                selectedIndex = selectedIndex,
                onPageSelected = { notebookViewModel.selectPage(it) }
            )

            PageEditor(
                page = notebookViewModel.pages.getOrNull(selectedIndex),
                pageNumber = selectedIndex + 1,
                onTitleChange = notebookViewModel::updateTitle,
                onContentChange = notebookViewModel::updateContent
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: () -> Unit,
    deleteEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Поиск по названию") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Button(onClick = onAddPage) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить страницу")
            }
            androidx.compose.material3.Button(
                onClick = onDeletePage,
                enabled = deleteEnabled
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Удалить выбранную")
            }
        }
    }
}

@Composable
fun PageList(
    pages: List<Pair<Int, PageEntry>>,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет страниц по заданному запросу")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(pages) { (index, page) ->
                    val isSelected = index == selectedIndex
                    PageListItem(
                        number = index + 1,
                        page = page,
                        isSelected = isSelected,
                        onClick = { onPageSelected(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun PageListItem(
    number: Int,
    page: PageEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Страница $number",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = page.title.ifEmpty { "(Без названия)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
        val previewText = if (page.content.isBlank()) {
            "Нет записи"
        } else {
            val snippet = page.content.trim()
            "${snippet.take(24)}${if (snippet.length > 24) "…" else ""}"
        }
        Text(
            text = previewText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PageEditor(
    page: PageEntry?,
    pageNumber: Int,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit
) {
    if (page == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Выберите страницу для редактирования")
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Страница №$pageNumber",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = page.title,
                onValueChange = onTitleChange,
                label = { Text("Название страницы") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = page.content,
                onValueChange = onContentChange,
                label = { Text("Запись") },
                modifier = Modifier
                    .fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(),
                singleLine = false,
                minLines = 6,
                maxLines = Int.MAX_VALUE
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotebookPreview() {
    val previewViewModel = remember { NotebookViewModel() }
    MaterialTheme {
        NotebookScreen(notebookViewModel = previewViewModel)
    }
}
