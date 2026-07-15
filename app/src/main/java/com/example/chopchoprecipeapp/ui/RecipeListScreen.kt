package com.example.chopchoprecipeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chopchoprecipeapp.data.Recipe

/**
 * A composable function that displays recipes in a searchable list.
 * @param viewModel the view model containing all recipes
 * @param onRecipeClick a lambda function that is called when a recipe is clicked
 * @param modifier the modifier to apply to this layout
 * @author Amelie Dzierzawa
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeListScreen(
    viewModel: RecipeViewModel,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val recipes by viewModel.allRecipes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    //Get all unique tags from recipes
    val allTags = remember(recipes) {
        recipes.flatMap { it.tags }.distinct().sorted()
    }

    Box(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            //Search and Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search recipes") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                //Filter button
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter by tags",
                        tint = if (selectedTags.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            //Filter dialog
            if (showFilterDialog) {
                AlertDialog(
                    onDismissRequest = { showFilterDialog = false },
                    title = { Text("Filter by Tags") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            if (allTags.isEmpty()) {
                                Text("No tags available")
                            } else {
                                allTags.forEach { tag ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = tag in selectedTags,
                                            onCheckedChange = { isChecked ->
                                                selectedTags = if (isChecked) {
                                                    selectedTags + tag
                                                } else {
                                                    selectedTags - tag
                                                }
                                            }
                                        )
                                        Text(
                                            text = tag,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFilterDialog = false }) {
                            Text("Done")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            selectedTags = setOf()
                            showFilterDialog = false
                        }) {
                            Text("Clear All")
                        }
                    }
                )
            }

            //Filter recipes based on search query and selected tags
            val filteredRecipes = recipes.filter { recipe ->
                val matchesSearch = if (searchQuery.isEmpty()) {
                    true
                } else {
                    recipe.name.contains(searchQuery, ignoreCase = true)
                }

                val matchesTags = if (selectedTags.isEmpty()) {
                    true
                } else {
                    selectedTags.any { tag -> tag in recipe.tags }
                }

                matchesSearch && matchesTags
            }

            if (recipes.isEmpty()) {
                Text(
                    text = "No recipes added yet!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (filteredRecipes.isEmpty()) {
                Text(
                    text = "No recipes match your criteria!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //Recipe count as first item
                    item {
                        if (filteredRecipes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${filteredRecipes.size} recipe${if (filteredRecipes.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    //after that the recipes
                    items(filteredRecipes, key = { it.id }) { recipe ->
                        RecipeCardWithSwipe(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe) },
                            onDelete = { viewModel.deleteRecipe(recipe) }
                        )
                    }
                }
            }
        }
    }
}


/**
 * Wraps a RecipeCard with swipe-to-dismiss functionality.
 * Triggers a delete confirmation dialog when swiped from left to right.
 * Swiping from right to left is disabled to prevent accidental moves.
 * @param recipe The recipe data to display and potentially delete.
 * @param onClick Callback triggered when the card is clicked.
 * @param onDelete Callback triggered when the user confirms recipe deletion.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeCardWithSwipe(
    recipe: Recipe,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Manage state for the swipe-to-dismiss box
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                showDeleteDialog = true
                false //Return false to prevent automatic dismissal before confirmation
            } else {
                false
            }
        }
    )

    //Reset swipe position if dialog is dismissed
    LaunchedEffect(showDeleteDialog) {
        if (!showDeleteDialog && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    //Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete \"${recipe.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    //Swipe to delete container
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        content = {
            RecipeCard(
                recipe = recipe,
                onClick = onClick
            )
        }
    )
}

/**
 * A composable function that displays a recipe card.
 * @param recipe the recipe to display
 * @param onClick a lambda function that is called when the recipe card is clicked
 * @author Amelie Dzierzawa
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            //Recipe image
            if (!recipe.imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = recipe.imageUri,
                    contentDescription = recipe.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "No image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                //Name and Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    // Stars
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = if (index < recipe.rating)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                //Tags
                if (recipe.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(text = tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}