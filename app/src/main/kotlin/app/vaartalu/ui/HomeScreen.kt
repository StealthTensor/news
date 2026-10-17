@file:OptIn(ExperimentalMaterial3Api::class)

package app.vaartalu.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import app.vaartalu.R
import app.vaartalu.data.Channel
import app.vaartalu.data.groups
import app.vaartalu.player.HomeState
import app.vaartalu.player.PlaybackStatus
import app.vaartalu.player.PlayerViewModel
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay

private const val GRID_COLUMNS = 3
private const val TILE_ASPECT = 0.95f
private const val WHITE_PLATE = 0xFFFFFFFF.toInt()
private val TileShape = RoundedCornerShape(16.dp)
private val PlateShape = RoundedCornerShape(10.dp)

@Composable
fun HomeScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Immersive(enabled = landscape)

    // null = follow the playing channel's tab; set only while he browses another tab.
    var pickedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = state.channels.groups
    val activeGroup = pickedGroup?.takeIf { it in groups } ?: state.selected?.group ?: groups.firstOrNull()
    val onSelect: (Channel) -> Unit = {
        pickedGroup = null
        viewModel.select(it)
    }

    if (landscape) {
        LandscapeLayout(state, viewModel.player, onSelect, viewModel::reload)
    } else {
        PortraitLayout(state, groups, activeGroup, { pickedGroup = it }, viewModel.player, onSelect, viewModel::reload)
    }
}

@Composable
private fun PortraitLayout(
    state: HomeState,
    groups: List<String>,
    activeGroup: String?,
    onGroup: (String) -> Unit,
    player: Player,
    onSelect: (Channel) -> Unit,
    onReload: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .safeDrawingPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
        ) {
            PlayerSurface(player, Modifier.fillMaxSize())
            StatusOverlay(state.status, onReload, Modifier.fillMaxSize())
        }
        NowPlayingBar(state, onReload)
        if (groups.size > 1) GroupTabs(groups, activeGroup, onGroup)
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.channels.filter { it.group == activeGroup }, key = { it.id }) { channel ->
                ChannelTile(
                    channel = channel,
                    isSelected = channel.id == state.selectedId,
                    onClick = { onSelect(channel) },
                    modifier = Modifier.aspectRatio(TILE_ASPECT),
                )
            }
        }
        Credit()
    }
}

@Composable
private fun LandscapeLayout(
    state: HomeState,
    player: Player,
    onSelect: (Channel) -> Unit,
    onReload: () -> Unit,
) {
    var chrome by remember { mutableStateOf(FullscreenChrome.Hidden) }
    LaunchedEffect(chrome) {
        chrome.autoHideMs?.let {
            delay(it)
            chrome = FullscreenChrome.Hidden
        }
    }
    BackHandler(enabled = chrome != FullscreenChrome.Hidden) { chrome = FullscreenChrome.Hidden }

    val stripState = rememberLazyListState()
    LaunchedEffect(chrome == FullscreenChrome.Channels) {
        val index = state.channels.indexOfFirst { it.id == state.selectedId }
        if (index >= 0) stripState.scrollToItem(index)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PlayerSurface(player, Modifier.fillMaxSize())
        // Transparent tap target above the video: PlayerView does not forward taps to Compose.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    chrome = chrome.onScreenTap()
                },
        )
        StatusOverlay(state.status, onReload, Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = chrome == FullscreenChrome.Buttons,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100)),
        ) {
            Row(
                Modifier.safeDrawingPadding().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PillButton(stringResource(R.string.channels), Icons.Default.Menu) {
                    chrome = chrome.onChannelsPressed()
                }
                PillButton(stringResource(R.string.reload), Icons.Default.Refresh) {
                    onReload()
                    chrome = FullscreenChrome.Hidden
                }
            }
        }

        AnimatedVisibility(
            visible = chrome == FullscreenChrome.Channels,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100)),
        ) {
            LazyRow(
                state = stripState,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                    .safeDrawingPadding()
                    .padding(top = 32.dp, bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.channels, key = { it.id }) { channel ->
                    ChannelTile(
                        channel = channel,
                        isSelected = channel.id == state.selectedId,
                        onClick = {
                            onSelect(channel)
                            chrome = chrome.onChannelPicked()
                        },
                        modifier = Modifier.width(112.dp).height(100.dp),
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSurface(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                this.player = player
            }
        },
        onRelease = { it.player = null },
    )
}

@Composable
private fun StatusOverlay(status: PlaybackStatus, onReload: () -> Unit, modifier: Modifier = Modifier) {
    if (status == PlaybackStatus.Playing) return
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.55f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (status) {
            PlaybackStatus.Loading -> {
                CircularProgressIndicator(color = Palette.Amber, strokeWidth = 4.dp)
                Spacer(Modifier.height(14.dp))
                OverlayText(stringResource(R.string.loading))
            }
            PlaybackStatus.Failed -> {
                OverlayText(stringResource(R.string.failed))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onReload,
                    modifier = Modifier.heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.OnAmber),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reload), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            PlaybackStatus.Playing -> Unit
        }
    }
}

@Composable
private fun OverlayText(text: String) {
    Text(text, color = Palette.Text, fontSize = 19.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
}

@Composable
private fun NowPlayingBar(state: HomeState, onReload: () -> Unit) {
    val channel = state.selected
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            StatusLine(state.status)
            Text(
                channel?.name.orEmpty(),
                color = Palette.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(
            onClick = onReload,
            modifier = Modifier.heightIn(min = 52.dp),
            border = BorderStroke(1.5.dp, Palette.TextDim),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Text),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.reload), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusLine(status: PlaybackStatus) {
    val (label, color) = when (status) {
        PlaybackStatus.Playing -> stringResource(R.string.status_live) to Palette.Live
        PlaybackStatus.Loading -> stringResource(R.string.status_connecting) to Palette.Amber
        PlaybackStatus.Failed -> stringResource(R.string.status_no_signal) to Palette.TextDim
    }
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .alpha(if (status == PlaybackStatus.Loading) pulse.value else 1f)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = Palette.TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

/** White = the tab being browsed. Amber is reserved for the channel that is playing. */
@Composable
private fun GroupTabs(groups: List<String>, active: String?, onPick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { group ->
            val isActive = group == active
            Surface(
                onClick = { onPick(group) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { selected = isActive },
                shape = CircleShape,
                color = if (isActive) Palette.Text else Palette.Panel,
            ) {
                Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        group,
                        color = if (isActive) Palette.Ink else Palette.Text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PillButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.75f),
        contentColor = Palette.Text,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
    ) {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChannelTile(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = channel.name
            selected = isSelected
        },
        shape = TileShape,
        color = if (isSelected) Palette.Amber else Palette.Panel,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoPlate(channel, Modifier.weight(1f).fillMaxWidth())
            Text(
                channel.name,
                color = if (isSelected) Palette.OnAmber else Palette.Text,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Logo on an opaque plate so every channel's artwork reads the same on the dark tile. */
@Composable
private fun LogoPlate(channel: Channel, modifier: Modifier = Modifier) {
    val plate = Color(channel.plateArgb ?: WHITE_PLATE)
    val monogramColor = if (plate.luminance() > 0.5f) Palette.Ink else Palette.Text
    Box(modifier.clip(PlateShape).background(plate), contentAlignment = Alignment.Center) {
        if (channel.logo == null) {
            Monogram(channel.name, monogramColor)
        } else {
            SubcomposeAsyncImage(
                model = channel.logo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit,
                loading = { Monogram(channel.name, monogramColor) },
                error = { Monogram(channel.name, monogramColor) },
            )
        }
    }
}

@Composable
private fun Monogram(name: String, color: Color) {
    Text(name.take(3).uppercase(), color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
}

@Composable
private fun Credit() {
    val credit = stringResource(R.string.credit)
    val loveStart = credit.indexOf("Love")
    Text(
        text = buildAnnotatedString {
            append(credit)
            if (loveStart >= 0) {
                addStyle(SpanStyle(color = Palette.Live, fontWeight = FontWeight.Bold), loveStart, loveStart + "Love".length)
            }
        },
        color = Palette.TextDim,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun Immersive(enabled: Boolean) {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    DisposableEffect(enabled) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        if (enabled) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}
