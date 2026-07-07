package com.example.chopchoprecipeapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chopchoprecipeapp.data.Recipe

//local helper structure for the separate metric input in the UI state
data class IngredientInput(
    val amount: String = "",
    val unit: String = "g",
    val name: String = ""
)

/**
 * A composable function that displays a screen for adding a new recipe.
 * @param viewModel the view model to use for adding the recipe
 * @param onRecipeSaved a lambda function that is called when the recipe is saved
 * @param modifier the modifier to apply to this layout
 * @author Amelie Dzierzawa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    viewModel: RecipeViewModel,
    onRecipeSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var ratingString by remember { mutableStateOf("5") }
    var tagsString by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    //image
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    //dynamic list of ingredients
    val ingredientsList = remember { mutableStateListOf(IngredientInput()) }

    //list of allowed metrics
    val metricUnits = listOf("g", "kg", "ml", "l", "Stk.", "TL", "EL", "Prise")

    //launcher for the android image gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Add New Recipe", style = MaterialTheme.typography.headlineMedium)
        }

        //Image Selection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) //image as square
                    .clickable { imagePickerLauncher.launch("image/*") }, //click on image opens gallery
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium //rounded edges
            ) {
                if (selectedImageUri != null) {
                    //shows selected image
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Chosen Recipe Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop //cuts image without distortion
                    )
                } else {
                    //placeholder if no image is selected
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📸", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Choose Image From Gallery", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        //Basic data
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Recipe Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = ratingString,
                onValueChange = { ratingString = it },
                label = { Text("Rating (1-5)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = tagsString,
                onValueChange = { tagsString = it },
                label = { Text("Tags (seperated by Komma, z.B. Veggie, Quick)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        //Ingredient section
        item {
            Text("Ingredients", style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(ingredientsList) { index, ingredient ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ingredient.amount,
                    onValueChange = { ingredientsList[index] = ingredient.copy(amount = it) },
                    placeholder = { Text("Amount") },
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight(),
                    maxLines = 1
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .weight(1.1f)
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
                        maxLines = 1
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

                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = { ingredientsList[index] = ingredient.copy(name = it) },
                    placeholder = { Text("Ingredient") },
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxHeight(),
                    maxLines = 1
                )

                if (ingredientsList.size > 1) {
                    IconButton(
                        onClick = { ingredientsList.removeAt(index) },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text("❌", color = MaterialTheme.colorScheme.error)
                    }
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
                    if (name.isNotBlank()) {
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

                        val newRecipe = Recipe(
                            id = 0,
                            name = name,
                            rating = ratingString.toIntOrNull() ?: 5,
                            tags = tagsList,
                            instructions = instructions,
                            ingredients = filteredIngredients,
                            imageUri = selectedImageUri?.toString()
                        )

                        viewModel.addRecipe(newRecipe)
                        onRecipeSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Save Recipe")
            }
        }
    }
}