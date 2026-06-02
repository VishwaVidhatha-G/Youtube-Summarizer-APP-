package com.summarizer.app.presentation.overlay

import androidx.activity.compose.BackHandler

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.summarizer.app.domain.model.ChatMessage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summarizer.app.core.designsystem.CardBorderColor
import com.summarizer.app.core.designsystem.GlassHighlight
import com.summarizer.app.presentation.MainActivity

/**
 * Premium Frosted Overlay Composable displaying active loading animations or complete summaries.
 */
@Composable
fun OverlayScreen(
    viewModel: OverlayViewModel,
    onDismiss: () -> Unit,
    onOpenMainApp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.summary) {
        if (uiState.summary == null) {
            selectedTab = 0
        }
    }

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    // Slide up bottom animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { 
                // Tap outside sheet to dismiss activity
                isVisible = false
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        
        // Bottom Sheet Frosted Canvas
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                // Consume clicks to prevent sheet dismissal, while preserving keyboard focus propagation
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f) // Premium viewport height
                    .border(1.dp, CardBorderColor, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    
                    // --- Bottom Sheet Drag Handle Bar ---
                    Box(
                        modifier = Modifier
                            .size(42.dp, 5.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Video Title Header ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                isVisible = false
                                onDismiss()
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss Overlay", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Scrollable Core Area ---
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when {
                            uiState.isLoading -> {
                                ShimmerLoader(uiState.statusMessage)
                            }
                            uiState.error != null -> {
                                ErrorState(
                                    error = uiState.error ?: "",
                                    isApiKeyMissing = uiState.isApiKeyMissing,
                                    onOpenMainApp = onOpenMainApp,
                                    onDismiss = onDismiss
                                )
                            }
                            uiState.summary != null -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    TabRow(
                                        selectedTabIndex = selectedTab,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) }
                                    ) {
                                        Tab(
                                            selected = selectedTab == 0,
                                            onClick = { selectedTab = 0 },
                                            text = { Text("Summary", fontWeight = FontWeight.SemiBold) }
                                        )
                                        Tab(
                                            selected = selectedTab == 1,
                                            onClick = { selectedTab = 1 },
                                            text = { Text("Chat with Video", fontWeight = FontWeight.SemiBold) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        if (selectedTab == 0) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(scrollState)
                                                    .scrollbar(scrollState, color = MaterialTheme.colorScheme.primary, thickness = 6.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                MarkdownText(
                                                    text = uiState.summary?.summary ?: "",
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                if (uiState.isGenerating) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "AI is typing...",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(24.dp))
                                            }
                                        } else {
                                            ChatTabContent(
                                                uiState = uiState,
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Footer Operations Bar (Visible only on Success) ---
                    if (uiState.summary != null && !uiState.isGenerating && selectedTab == 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Regenerate Button
                            Button(
                                onClick = {
                                    val videoUrl = uiState.videoId?.let { "https://youtube.com/watch?v=$it" }
                                    viewModel.loadSummary(videoUrl, forceRegenerate = true)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-run", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }

                            // Copy to Clipboard
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Video Summary", uiState.summary?.summary ?: "")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            
                            // Share Bullet Points
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, uiState.title)
                                        putExtra(Intent.EXTRA_TEXT, uiState.summary?.summary ?: "")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Summary via"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium skeletal pulsing loading state representation.
 */
@Composable
private fun ShimmerLoader(status: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(56.dp)
                .alpha(alphaAnim)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = status,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pulsing skeleton bars representation
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .alpha(alphaAnim),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
            Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
            Box(modifier = Modifier.fillMaxWidth(0.92f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
        }
    }
}

/**
 * Clean UI styling for warning/error sheets.
 */
@Composable
private fun ErrorState(
    error: String,
    isApiKeyMissing: Boolean,
    onOpenMainApp: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isApiKeyMissing) Icons.Default.LockReset else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = if (isApiKeyMissing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isApiKeyMissing) "Setup Key Required" else "Summarization Interrupted",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(28.dp))

        if (isApiKeyMissing) {
            Button(
                onClick = onOpenMainApp,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configure API Key", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Close Overlay", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Custom scrollbar modifier since Compose Android does not provide a visible scrollbar by default.
 */
fun Modifier.scrollbar(
    state: ScrollState,
    color: Color,
    alpha: Float = 0.5f,
    thickness: androidx.compose.ui.unit.Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()

    if (state.maxValue > 0) {
        val contentHeight = size.height
        val viewportHeight = contentHeight - state.maxValue.toFloat()
        val thumbHeight = viewportHeight * (viewportHeight / contentHeight)
        
        val thumbYInViewport = (state.value.toFloat() / state.maxValue.toFloat()) * (viewportHeight - thumbHeight)
        val thumbY = state.value.toFloat() + thumbYInViewport

        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - thickness.toPx(), thumbY),
            size = Size(thickness.toPx(), thumbHeight),
            alpha = alpha,
            cornerRadius = CornerRadius(thickness.toPx() / 2f, thickness.toPx() / 2f)
        )
    }
}

@Composable
private fun ChatTabContent(
    uiState: OverlayUiState,
    viewModel: OverlayViewModel
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ask questions based on the video transcript",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Clear",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { viewModel.clearChatHistory() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.isChatLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scraping transcript for chat...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.chatMessages.size) { index ->
                        val message = uiState.chatMessages[index]
                        ChatMessageBubble(message)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.chatInputText,
                onValueChange = { viewModel.updateChatInput(it) },
                placeholder = { Text("Ask about this video...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            IconButton(
                onClick = { 
                    viewModel.sendChatMessage(uiState.chatInputText)
                },
                enabled = uiState.chatInputText.isNotBlank() && !uiState.isChatLoading,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColors = if (message.isUser) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    }
    val contentColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = bgColors,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.isUser) {
                    Text(
                        text = message.content,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    MarkdownText(
                        text = message.content,
                        color = contentColor
                    )
                }
                
                if (message.isStreaming && message.content.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(contentColor, RoundedCornerShape(50)).alpha(0.5f))
                        Box(modifier = Modifier.size(6.dp).background(contentColor, RoundedCornerShape(50)).alpha(0.5f))
                        Box(modifier = Modifier.size(6.dp).background(contentColor, RoundedCornerShape(50)).alpha(0.5f))
                    }
                }
            }
        }
    }
}
