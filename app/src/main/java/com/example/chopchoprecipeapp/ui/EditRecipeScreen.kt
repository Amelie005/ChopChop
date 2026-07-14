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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.utils.ImageManager

/**
 * A composable function that displays a screen for editing an existing recipe.
 * @param recipe the recipe to edit
 * @param viewModel the view model to use for editing the recipe
 * @param onRecipeSaved a lambda function that is called when the recipe is saved
 * @param onBackClick a lambda function that is called when the back button is pressed
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

    //image
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUri by remember { mutableStateOf(recipe.imageUri) }

    // Parse ingredients from the recipe format back into IngredientInput
    val ingredientsList = remember {
        mutableStateListOf(
            *recipe.ingredients.map { ingredientString ->
                // Parse ingredient string like "100 g flour" or "2 cups sugar" or "flour"
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

    //list of allowed metrics
    val metricUnits = listOf("g", "kg", "ml", "l", "pc.", "ts", "tbs", "pich", "cup", "ounce")

    //launcher for the android image gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

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
                        //shows newly selected image
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Chosen Recipe Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop //cuts image without distortion
                        )
                    } else if (!currentImageUri.isNullOrEmpty()) {
                        //shows existing image
                        AsyncImage(
                            model = currentImageUri,
                            contentDescription = "Current Recipe Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        //placeholder if no image
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Add Photo",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Choose Image From Gallery",
                                    style = MaterialTheme.typography.bodyMedium
                                )
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
                            Text("X", color = MaterialTheme.colorScheme.error)
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
                                    val amountPart =
                                        if (input.amount.isNotBlank()) "${input.amount} " else ""
                                    val unitPart =
                                        if (input.unit.isNotBlank() && input.amount.isNotBlank()) "${input.unit} " else ""
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
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
