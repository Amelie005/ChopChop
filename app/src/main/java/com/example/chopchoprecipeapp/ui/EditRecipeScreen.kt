package com.example.chopchoprecipeapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.utils.ImageManager

/**
 * A composable function that displays a screen for editing an existing recipe.
 * Similar to AddRecipeScreen.
 * @param recipe the recipe to edit
 * @param viewModel the view model to handle recipe updates
 * @param onRecipeSaved callback when changes are saved
 * @param onBackClick callback when navigating back
 * @param modifier the modifier to apply to this layout
 * @author Amelie Dzierzawa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    onRecipeSaved: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(recipe.name) }
    var ratingString by remember { mutableStateOf(recipe.rating.toString()) }
    var tagsString by remember { mutableStateOf(recipe.tags.joinToString(", ")) }
    var instructions by remember { mutableStateOf(recipe.instructions) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUri by remember { mutableStateOf(recipe.imageUri) }
    var submitted by remember { mutableStateOf(false) }

    //Parse ingredients from the recipe format back into IngredientInput
    val ingredientsList = remember {
        mutableStateListOf(
            *recipe.ingredients.map { ingredientString ->
                val parts = ingredientString.trim().split(" ", limit = 3)
                when {
                    parts.size >= 3 -> IngredientInput(
                        amount = parts[0],
                        unit = parts[1],
                        name = parts.drop(2).joinToString(" ")
                    )
                    parts.size == 2 -> IngredientInput(
                        amount = parts[0],
                        unit = "g",
                        name = parts.drop(1).joinToString(" ")
                    )
                    else -> IngredientInput(name = ingredientString)
                }
            }.toTypedArray()
        )
    }

    val metricUnits = listOf(
        "g", "kg", "ml", "l",
        "oz", "lb", "cup", "tbsp", "tsp", "fl oz",
        "Stk.", "pcs"
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    //validation, only shown if submitted is true
    val nameError = when {
        name.isBlank() -> "Recipe name is required!"
        name.length > 500 -> "Recipe name too long (max 500 characters)!"
        else -> null
    }

    val ratingError = when {
        ratingString.toIntOrNull() == null -> "Invalid rating!"
        ratingString.toInt() !in 1..5 -> "Rating must be between 1 and 5!"
        else -> null
    }

    val ingredientsError = when {
        ingredientsList.all { it.name.isBlank() } -> "Add at least one ingredient!"
        else -> null
    }

    val amountErrors = ingredientsList.mapIndexed { index, ingredient ->
        if (ingredient.amount.isNotBlank() && ingredient.amount.toDoubleOrNull() == null) {
            index to "Only numbers allowed!"
        } else {
            null
        }
    }.filterNotNull()

    val isFormValid = nameError == null &&
            ratingError == null &&
            ingredientsError == null &&
            amountErrors.isEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Recipe", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //Image selection
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Chosen Recipe Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!currentImageUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentImageUri,
                            contentDescription = "Current Recipe Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Add image",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Choose Image From Gallery", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            //Recipe name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 500) name = it },
                    label = { Text("Recipe Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = submitted && nameError != null,
                    supportingText = {
                        if (submitted && nameError != null) {
                            Text(nameError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${name.length}/500", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }

            //Rating (selection via stars)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Rating", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) { index ->
                            IconButton(
                                onClick = { ratingString = (index + 1).toString() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star ${index + 1}",
                                    tint = if (index < (ratingString.toIntOrNull() ?: 0))
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    if (submitted && ratingError != null) {
                        Text(
                            ratingError,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            //Tags
            item {
                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("e.g. Vegetarian, Quick, Healthy") }
                )
            }

            //Ingredients
            item {
                Text("Ingredients", style = MaterialTheme.typography.titleMedium)
            }

            itemsIndexed(ingredientsList) { index, ingredient ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //Amount
                            OutlinedTextField(
                                value = ingredient.amount,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                                        ingredientsList[index] = ingredient.copy(amount = newValue)
                                    }
                                },
                                placeholder = { Text("AMT") },
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight(),
                                maxLines = 1,
                                isError = submitted && amountErrors.any { it.first == index },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            //Unit
                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                            ) {
                                OutlinedTextField(
                                    value = ingredient.unit,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxHeight(),
                                    maxLines = 1,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    metricUnits.forEach { unitOption ->
                                        DropdownMenuItem(
                                            text = { Text(unitOption) },
                                            onClick = {
                                                ingredientsList[index] = ingredient.copy(unit = unitOption)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            //Ingredient name
                            OutlinedTextField(
                                value = ingredient.name,
                                onValueChange = { ingredientsList[index] = ingredient.copy(name = it) },
                                placeholder = { Text("Ingredient") },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .fillMaxHeight(),
                                maxLines = 1,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            //Delete button
                            if (ingredientsList.size > 1) {
                                IconButton(
                                    onClick = { ingredientsList.removeAt(index) },
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Delete ingredient",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        //Error message below
                        if (submitted && amountErrors.any { it.first == index }) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Only numbers allowed!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            //Ingredients error
            if (submitted && ingredientsError != null) {
                item {
                    Text(
                        ingredientsError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            //Amount errors
            if (submitted) {
                amountErrors.forEach { (index, error) ->
                    item {
                        Text(
                            "Ingredient ${index + 1}: $error",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { ingredientsList.add(IngredientInput()) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("+ Add Ingredient")
                }
            }

            //Instructions
            item {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            //Save button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        submitted = true

                        if (isFormValid) {
                            val tagsList = tagsString.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            val filteredIngredients = ingredientsList
                                .filter { it.name.isNotBlank() }
                                .map { input ->
                                    val amountPart = if (input.amount.isNotBlank()) "${input.amount} " else ""
                                    val unitPart = if (input.unit.isNotBlank() && input.amount.isNotBlank()) "${input.unit} " else ""
                                    "$amountPart$unitPart${input.name}".trim()
                                }

                            val finalImageUri = if (selectedImageUri != null) {
                                ImageManager.saveImageFromUri(context, selectedImageUri!!)
                            } else {
                                currentImageUri
                            }

                            val updatedRecipe = Recipe(
                                id = recipe.id,
                                name = name,
                                rating = ratingString.toIntOrNull() ?: 5,
                                tags = tagsList,
                                instructions = instructions,
                                ingredients = filteredIngredients,
                                imageUri = finalImageUri
                            )

                            viewModel.editRecipe(updatedRecipe)
                            onRecipeSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
