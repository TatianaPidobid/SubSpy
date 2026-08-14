package com.subspy.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.subspy.app.R
import com.subspy.app.data.model.CancellationDatabase
import com.subspy.app.data.model.Subscription
import com.subspy.app.ui.theme.ForgottenRed
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.ui.theme.SuccessGreen
import com.subspy.app.ui.theme.TextOnGreen
import com.subspy.app.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.util.Locale

private val BUTTON_HEIGHT = 68.dp
private val BODY_SIZE = 19.sp
private val TITLE_SIZE = 26.sp

private enum class CancelStep {
    OPEN_PAGE,
    ASK_RESULT,
    SUCCESS,
    LETTER,
    BANK
}

/**
 * Guided, one-action-per-screen cancellation flow designed for users who are
 * not comfortable with technology: large text, full-width buttons, no choices
 * except the single "did it work?" question.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelFlowScreen(
    subscriptionId: String,
    subscriptionViewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val subscriptions by subscriptionViewModel.subscriptions.collectAsState()
    val subscription = subscriptions.find { it.id == subscriptionId }
    var step by remember { mutableStateOf(CancelStep.OPEN_PAGE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cancel_subscription),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (subscription == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.error_occurred),
                    fontSize = BODY_SIZE,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val info = remember(subscription.serviceName) {
                CancellationDatabase.getCancellationInfo(subscription.serviceName)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (step) {
                    CancelStep.OPEN_PAGE -> OpenPageStep(
                        serviceName = subscription.serviceName,
                        tapHint = info.tapHint,
                        url = info.cancellationUrl.ifBlank {
                            "https://www.google.com/search?q=" +
                                Uri.encode("${subscription.serviceName} cancel subscription")
                        },
                        onDone = { step = CancelStep.ASK_RESULT }
                    )

                    CancelStep.ASK_RESULT -> AskResultStep(
                        onYes = {
                            subscriptionViewModel.markCancelled(subscription.id)
                            step = CancelStep.SUCCESS
                        },
                        onNo = { step = CancelStep.LETTER }
                    )

                    CancelStep.SUCCESS -> SuccessStep(
                        yearlySavings = subscription.monthlyAmount * 12,
                        onDone = onBack
                    )

                    CancelStep.LETTER -> LetterStep(
                        subscription = subscription,
                        supportEmail = info.supportEmail,
                        supportUrl = info.supportUrl,
                        onNotHelped = { step = CancelStep.BANK }
                    )

                    CancelStep.BANK -> BankStep(
                        serviceName = subscription.serviceName,
                        onDone = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenPageStep(
    serviceName: String,
    tapHint: String,
    url: String,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = GreenAccent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = TextOnGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.cancel_tap_here),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextOnGreen
                    )
                    Text(
                        text = if (tapHint.isBlank()) {
                            stringResource(R.string.cancel_tap_generic, serviceName)
                        } else {
                            stringResource(R.string.cancel_tap_button, tapHint)
                        },
                        fontSize = BODY_SIZE,
                        color = TextOnGreen
                    )
                }
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            }
        )

        BigButton(
            text = stringResource(R.string.cancel_done_on_site),
            onClick = onDone,
            containerColor = GreenAccent,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AskResultStep(onYes: () -> Unit, onNo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.cancel_did_it_work),
            fontSize = TITLE_SIZE,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(40.dp))
        BigButton(
            text = stringResource(R.string.answer_yes),
            onClick = onYes,
            containerColor = GreenAccent
        )
        Spacer(modifier = Modifier.height(20.dp))
        BigButton(
            text = stringResource(R.string.answer_no),
            onClick = onNo,
            containerColor = ForgottenRed
        )
    }
}

@Composable
private fun SuccessStep(yearlySavings: Double, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.cancel_congrats),
            fontSize = TITLE_SIZE,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                R.string.cancel_savings,
                formatMoneyPlain(yearlySavings)
            ),
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = SuccessGreen
        )
        Spacer(modifier = Modifier.height(40.dp))
        BigButton(
            text = stringResource(R.string.done),
            onClick = onDone,
            containerColor = GreenAccent
        )
    }
}

@Composable
private fun LetterStep(
    subscription: Subscription,
    supportEmail: String,
    supportUrl: String,
    onNotHelped: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val subject = stringResource(R.string.letter_subject, subscription.serviceName)
    val body = stringResource(
        R.string.letter_body,
        subscription.serviceName,
        formatMoneyPlain(subscription.amount),
        subscription.currency,
        subscription.firstPaymentDate.ifBlank { subscription.nextBillingDate },
        subscription.lastPaymentDate.ifBlank { subscription.nextBillingDate }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.letter_title),
            fontSize = TITLE_SIZE,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = body,
                fontSize = BODY_SIZE,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BigButton(
            text = if (supportEmail.isNotBlank()) {
                stringResource(R.string.letter_send)
            } else {
                stringResource(R.string.letter_copy_and_open)
            },
            onClick = {
                if (supportEmail.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$supportEmail")
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        clipboard.setText(AnnotatedString("$subject\n\n$body"))
                    }
                } else {
                    clipboard.setText(AnnotatedString("$subject\n\n$body"))
                    val page = supportUrl.ifBlank {
                        "https://www.google.com/search?q=" +
                            Uri.encode("${subscription.serviceName} support contact")
                    }
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)))
                }
            },
            containerColor = GreenAccent
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onNotHelped,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.letter_did_not_help),
                fontSize = BODY_SIZE,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BankStep(serviceName: String, onDone: () -> Unit) {
    val steps = listOf(
        stringResource(R.string.bank_step_1),
        stringResource(R.string.bank_step_2, serviceName),
        stringResource(R.string.bank_step_3),
        stringResource(R.string.bank_step_4)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.bank_title),
            fontSize = TITLE_SIZE,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        steps.forEachIndexed { index, text ->
            Row(modifier = Modifier.padding(bottom = 20.dp)) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenAccent
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = text,
                    fontSize = BODY_SIZE,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        BigButton(
            text = stringResource(R.string.done),
            onClick = onDone,
            containerColor = GreenAccent
        )
    }
}

@Composable
private fun BigButton(
    text: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT)
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextOnGreen
        )
    }
}

private fun formatMoneyPlain(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(amount)
