package com.izyver.hashcode

import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.system.measureTimeMillis

const val basic = "input/b_basic.in.txt"
const val coarse = "input/c_coarse.in.txt"
const val difficult = "input/d_difficult.in.txt"
const val elaborate = "input/e_elaborate.in.txt"

fun main() {
    val input: List<Wish> = parseInput(File(difficult))
    val groupedWishes = groupWishes(input)
    println(calculateScore(input, groupedWishes.first().likes))
}

data class Wish(val likes: Set<String>, val dislikes: Set<String>)

fun groupWishes(wishes: List<Wish>): List<Wish> {
    val sortedWishes = wishes.sortedWith { o1, o2 -> (o1.dislikes.size - o1.likes.size) - (o2.dislikes.size - o2.likes.size) }
    val groupedWishes = ArrayList<Wish>()
    client@ for (wish in sortedWishes) {
        group@ for ((index, group) in groupedWishes.withIndex()) {
            val likesConflict: Boolean = wish.likes.fold(false) { acc, str -> group.dislikes.contains(str) || acc }
            val dislikesConflict: Boolean = wish.dislikes.fold(false) { acc, str -> group.likes.contains(str) || acc }
            if (!likesConflict && !dislikesConflict){
                groupedWishes[index] = Wish(group.likes + wish.likes, group.dislikes + wish.dislikes)
                continue@client
            }
        }
        groupedWishes.add(wish)
    }
    return groupedWishes
}

fun parseInput(file: File): List<Wish> {
    val lines = file.readLines()
    val num = lines.first().trim().toInt()
    var index = 0
    val output = ArrayList<Wish>(num)
    while (num * 2 > index) {
        val liked = lines[++index].split(" ")
        val disliked = lines[++index].split(" ")
        output.add(Wish(liked.subList(1, liked.size).toSet(), disliked.subList(1, disliked.size).toSet()))
    }
    return output
}

fun calculateScore (wishes: Collection<Wish>, recipe: Set<String>): Int {
    return wishes.fold(0) { score, wish ->
        val wishFits = recipe.containsAll(wish.likes) && !wish.dislikes.fold(false) { b, dis -> recipe.contains(dis) || b }
        if (wishFits) score + 1 else score
    }
}

fun bogdan(wishes: List<Wish>) {
    var maxScore = 0;
    var bestRecipe =  HashSet<String>();
    fun checkBest (recipe: HashSet<String>) {
        val score = calculateScore(wishes, recipe)
        if (score > maxScore) {
            maxScore = score
            bestRecipe = recipe;
        }
    }
    for (wish in wishes) {
        val needToAdd = wish.likes.filter { like -> !bestRecipe.contains(like) }
        val needToRemove = wish.dislikes.filter{like -> bestRecipe.contains(like) }
        val recipe = HashSet<String>(bestRecipe);
        needToAdd.forEach{item -> recipe.add(item)}
        needToRemove.forEach{item -> recipe.remove(item)}
        checkBest(recipe)
    }
    println("$maxScore")
}