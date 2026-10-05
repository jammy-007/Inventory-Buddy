package com.kitcheninventory.ui.items

import com.kitcheninventory.data.db.CategoryEntity
import com.kitcheninventory.data.db.UnitEntity

/** A common kitchen item that can be added with one tap. [unit] is a unit abbreviation. */
data class QuickItem(val name: String, val unit: String, val category: String)

private fun group(category: String, vararg items: Pair<String, String>) =
    items.map { (name, unit) -> QuickItem(name, unit, category) }

/** Grouped by the default categories, in the order they appear in the quick-add list. */
val quickItems: List<QuickItem> = listOf(
    group(
        "Produce",
        "Tomatoes" to "kg", "Onions" to "kg", "Potatoes" to "kg", "Cabbage" to "pc", "Carrots" to "kg",
        "Apples" to "kg", "Bananas" to "dz", "Lemons" to "pc", "Garlic" to "kg", "Ginger" to "kg",
        "Green chillies" to "kg", "Coriander" to "kg", "Lettuce" to "pc", "Cucumbers" to "kg",
        "Bell peppers" to "kg", "Spinach" to "kg", "Mushrooms" to "kg",
    ),
    group(
        "Meat & Poultry",
        "Chicken" to "kg", "Chicken breast" to "kg", "Mutton" to "kg", "Beef" to "kg", "Pork" to "kg",
        "Bacon" to "pack", "Sausages" to "pack",
    ),
    group("Seafood", "Fish fillet" to "kg", "Prawns" to "kg", "Squid" to "kg"),
    group(
        "Dairy & Eggs",
        "Milk" to "L", "Butter" to "kg", "Cheese" to "kg", "Paneer" to "kg", "Yogurt" to "kg",
        "Cream" to "L", "Eggs" to "dz",
    ),
    group(
        "Dry Goods",
        "Rice" to "kg", "Flour" to "kg", "Sugar" to "kg", "Salt" to "kg", "Pasta" to "kg",
        "Lentils" to "kg", "Oats" to "kg", "Bread crumbs" to "kg",
    ),
    group(
        "Spices & Condiments",
        "Cooking oil" to "L", "Olive oil" to "L", "Black pepper" to "g", "Turmeric" to "g",
        "Chilli powder" to "g", "Cumin" to "g", "Ketchup" to "btl", "Soy sauce" to "btl",
        "Vinegar" to "btl", "Mayonnaise" to "kg",
    ),
    group("Frozen", "Frozen fries" to "kg", "Frozen peas" to "kg", "Ice cream" to "L"),
    group("Bakery", "Bread" to "pc", "Burger buns" to "pack", "Pizza base" to "pack", "Tortillas" to "pack"),
    group(
        "Beverages",
        "Coffee beans" to "kg", "Tea" to "kg", "Soft drinks" to "can", "Bottled water" to "btl",
        "Orange juice" to "L",
    ),
    group(
        "Cleaning & Supplies",
        "Dish soap" to "btl", "Napkins" to "pack", "Garbage bags" to "pack", "Gloves" to "box",
        "Takeaway containers" to "pack", "Aluminium foil" to "pack",
    ),
).flatten()

/**
 * Fills the name, unit and category from [quick], leaving cost, stock and the rest as typed.
 * A unit or category that no longer exists is left unchanged.
 */
fun ItemForm.withQuickItem(quick: QuickItem, units: List<UnitEntity>, categories: List<CategoryEntity>): ItemForm =
    copy(
        name = quick.name,
        unitId = units.firstOrNull { it.abbreviation == quick.unit }?.id ?: unitId,
        categoryId = categories.firstOrNull { it.name == quick.category }?.id ?: categoryId,
    )
