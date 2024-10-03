package com.example.hangman

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.hangman.ui.theme.HangmanTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangmanTheme {
                Surface() {

                    Hangman()


                }
            }
        }
    }
}


@Composable
fun LetterButtons(
    guessedLetters: Set<Char>,
    disabledLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit
) {
    val alphabet = ('A'..'Z').toList()
    Column {
        // Display letters in rows
        alphabet.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { letter ->
                    val isGuessed = guessedLetters.contains(letter)
                    val isDisabled = disabledLetters.contains(letter)
                    TextButton(
                        onClick = { if (!isGuessed && !isDisabled) onLetterSelected(letter) },
                        enabled = !isGuessed && !isDisabled
                    ) {
                        Text(text = letter.toString())
                    }
                }
            }
        }
    }
}





@Composable
fun Hangman(){
    val currentWord = rememberSaveable { mutableStateOf("APPLEPIE") }
    val guessedLetters = rememberSaveable { mutableStateOf(mutableSetOf<Char>()) }
    val incorrectGuessCount = rememberSaveable { mutableStateOf(0) }
    val hintClickCount = rememberSaveable { mutableStateOf(0) }
    val hintText = rememberSaveable { mutableStateOf("") }
    val toastMessage = rememberSaveable { mutableStateOf("") }
    val disabledLetters = rememberSaveable { mutableStateOf(mutableSetOf<Char>()) }
    val isGameOver = rememberSaveable { mutableStateOf(false) }
    val hasWon = rememberSaveable { mutableStateOf(false) }
    val maxIncorrectGuesses = 7

    fun checkGameState() {
        if (!isGameOver.value) {
            if (currentWord.value.all { guessedLetters.value.contains(it) }) {
                hasWon.value = true
                isGameOver.value = true
            } else if (incorrectGuessCount.value >= maxIncorrectGuesses) {
                isGameOver.value = true
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(guessedLetters.value, incorrectGuessCount.value) {
        if (!isGameOver.value) {
            if (currentWord.value.all { guessedLetters.value.contains(it) }) {
                hasWon.value = true
                isGameOver.value = true
            }
            else if (incorrectGuessCount.value >= maxIncorrectGuesses) {
                isGameOver.value = true
            }
        }
    }

    LaunchedEffect(toastMessage.value) {
        if (toastMessage.value.isNotEmpty()) {
            snackbarHostState.showSnackbar(toastMessage.value)
            toastMessage.value = "Cannot Access hint"
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()

        ){

            if (isGameOver.value) {
                Column {
                    GameOverMessage(hasWon = hasWon.value)
                    NewGameButton(onNewGame = {
                        resetGame(currentWord, guessedLetters, incorrectGuessCount, isGameOver, hasWon, hintClickCount, hintText, disabledLetters)
                    })
                }
            }
            else{
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LetterButtons(guessedLetters = guessedLetters.value,
                        disabledLetters = disabledLetters.value) { letter ->
                        if (!isGameOver.value) {
                            guessedLetters.value = guessedLetters.value.toMutableSet().apply { add(letter) }
                            if (currentWord.value.contains(letter)) {
                                snackbarMessage = "Correct guess!"
                            } else {
                                incorrectGuessCount.value += 1
                                snackbarMessage = "Incorrect guess!"
                            }
                            checkGameState()
                        }
                    }

                    LaunchedEffect(snackbarMessage) {
                        if (snackbarMessage.isNotEmpty()) {
                            snackbarHostState.showSnackbar(snackbarMessage)
                            snackbarMessage = ""
                        }
                    }
                    TextButton(
                        onClick = {
                            if (incorrectGuessCount.value >= maxIncorrectGuesses - 1) {
                                toastMessage.value = "Hint not available"
                            } else {
                                if (hintClickCount.value == 0) {
                                    hintText.value = "Hint: FOOD"
                                    incorrectGuessCount.value += 1
                                } else if (hintClickCount.value == 1) {
                                    disableIncorrectLetters(currentWord.value, guessedLetters, disabledLetters)  // Disable incorrect letters
                                    incorrectGuessCount.value += 1
                                }
                                hintClickCount.value += 1
                            }
                        },
                        enabled = hintClickCount.value < 2
                    ) {
                        Text(text = if (hintClickCount.value == 0) "Use Hint" else "Use Second Hint")
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = hintText.value
                        )
                    }

                }

            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                HangmanDrawing(incorrectGuessCount = incorrectGuessCount.value)
                DisplayWord(currentWord.value, guessedLetters.value)
                if (isGameOver.value) {
                    GameOverMessage(hasWon = hasWon.value)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (isGameOver.value) {
                Column {
                    GameOverMessage(hasWon = hasWon.value)
                    NewGameButton(onNewGame = {
                        resetGame(currentWord, guessedLetters, incorrectGuessCount, isGameOver, hasWon, hintClickCount, hintText, disabledLetters)
                    })
                }
            }

            else{
                HangmanDrawing(incorrectGuessCount = incorrectGuessCount.value)
                DisplayWord(currentWord.value, guessedLetters.value)
                LetterButtons(guessedLetters = guessedLetters.value,
                    disabledLetters = disabledLetters.value) { letter ->
                    if (!isGameOver.value) {
                        guessedLetters.value = guessedLetters.value.toMutableSet().apply { add(letter) }
                        if (currentWord.value.contains(letter)) {
                            snackbarMessage = "Correct guess!"
                        } else {
                            incorrectGuessCount.value += 1
                            snackbarMessage = "Incorrect guess!"
                        }
                        checkGameState()
                    }
                }
            }



            LaunchedEffect(snackbarMessage) {
                if (snackbarMessage.isNotEmpty()) {
                    snackbarHostState.showSnackbar(snackbarMessage)
                    snackbarMessage = ""
                }
            }
            if (isGameOver.value) {
                GameOverMessage(hasWon = hasWon.value)
            }
        }
    }


    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(8.dp),
        snackbar = { Snackbar { Text(text = snackbarMessage) } }
    )



}



@Composable
fun DisplayWord(currentWord: String, guessedLetters: Set<Char>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        currentWord.forEach { letter ->
            Text(
                text = if (guessedLetters.contains(letter)) letter.toString() else "_",
                style = MaterialTheme.typography.displayMedium
            )
        }
    }
}

@Composable
fun GameOverMessage(hasWon: Boolean) {
    val message = if (hasWon) "You Win!" else "You Lose!"
    val color = if (hasWon) Color.Green else Color.Red

    Text(
        text = message,
        fontSize = 24.sp,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )
}




@Composable
fun NewGameButton(onNewGame: () -> Unit) {
    TextButton(onClick = onNewGame, modifier = Modifier.padding(top = 16.dp)) {
        Text(text = "New Game", fontSize = 20.sp)
    }
}

fun resetGame(
    currentWord: MutableState<String>,
    guessedLetters: MutableState<MutableSet<Char>>,
    incorrectGuessCount: MutableState<Int>,
    isGameOver: MutableState<Boolean>,
    hasWon: MutableState<Boolean>,
    hintClickCount: MutableState<Int>,
    hintText: MutableState<String>,
    disabledLetters: MutableState<MutableSet<Char>>
) {
    currentWord.value = getNewRandomWord()
    guessedLetters.value = mutableSetOf()
    incorrectGuessCount.value = 0
    isGameOver.value = false
    hasWon.value = false
    hintClickCount.value = 0
    hintText.value = ""
    disabledLetters.value = mutableSetOf()
}

fun getNewRandomWord(): String {
    val words = listOf("ORANGE", "BANANA", "GRAPES", "APPLE", "WATERMELON")
    return words.random()
}


fun disableIncorrectLetters(
    currentWord: String,
    guessedLetters: MutableState<MutableSet<Char>>,
    disabledLetters: MutableState<MutableSet<Char>>
) {
    val remainingLetters = ('A'..'Z').toSet().subtract(guessedLetters.value).subtract(currentWord.toSet())

    val lettersToDisable = remainingLetters.shuffled().take(remainingLetters.size / 2)

    disabledLetters.value.addAll(lettersToDisable)
}




//GPT code for hangman drawing

@Composable
fun HangmanDrawing(incorrectGuessCount: Int) {
    Canvas(modifier = Modifier.fillMaxSize(0.5f)) {
        // Gallows
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.75f, y = size.height * 0.1f),
            end = Offset(x = size.width * 0.75f, y = size.height * 0.7f),
            strokeWidth = 8f
        )
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.75f, y = size.height * 0.1f),
            end = Offset(x = size.width * 0.5f, y = size.height * 0.1f),
            strokeWidth = 8f
        )
        drawLine(
            color = Color.Black,
            start = Offset(x = size.width * 0.5f, y = size.height * 0.1f),
            end = Offset(x = size.width * 0.5f, y = size.height * 0.2f),
            strokeWidth = 8f
        )

        // Draw parts of the hangman based on incorrectGuessCount
        if (incorrectGuessCount > 0) {
            // Head
            drawCircle(
                color = Color.Black,
                center = Offset(x = size.width * 0.5f, y = size.height * 0.25f),
                radius = size.minDimension * 0.05f,
                style = Stroke(width = 8f)
            )
        }
        if (incorrectGuessCount > 1) {
            // Body
            drawLine(
                color = Color.Black,
                start = Offset(x = size.width * 0.5f, y = size.height * 0.3f),
                end = Offset(x = size.width * 0.5f, y = size.height * 0.45f),
                strokeWidth = 8f
            )
        }
        if (incorrectGuessCount > 2) {
            // Left Arm
            drawLine(
                color = Color.Black,
                start = Offset(x = size.width * 0.5f, y = size.height * 0.35f),
                end = Offset(x = size.width * 0.4f, y = size.height * 0.4f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
        if (incorrectGuessCount > 3) {
            // Right Arm
            drawLine(
                color = Color.Black,
                start = Offset(x = size.width * 0.5f, y = size.height * 0.35f),
                end = Offset(x = size.width * 0.6f, y = size.height * 0.4f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
        if (incorrectGuessCount > 4) {
            // Left Leg
            drawLine(
                color = Color.Black,
                start = Offset(x = size.width * 0.5f, y = size.height * 0.45f),
                end = Offset(x = size.width * 0.4f, y = size.height * 0.55f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
        if (incorrectGuessCount > 5) {
            // Right Leg
            drawLine(
                color = Color.Black,
                start = Offset(x = size.width * 0.5f, y = size.height * 0.45f),
                end = Offset(x = size.width * 0.6f, y = size.height * 0.55f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

    }
}


