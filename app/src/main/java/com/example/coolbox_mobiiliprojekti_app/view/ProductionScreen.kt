package com.example.coolbox_mobiiliprojekti_app.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coolbox_mobiiliprojekti_app.R
import com.example.coolbox_mobiiliprojekti_app.viewmodel.ProductionViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Job
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale


// Aikavälin luetelman määrittely
enum class TimeInterval {
    DAYS, HOURS, WEEKS, MONTHS, MAIN
}

enum class ProductionTypeInterval {
    Solar, Wind, Total
}

fun LocalDate.startOfWeek(): LocalDate {
    return this.minusDays(this.dayOfWeek.value.toLong() - 1)
}

/*
fun LocalDate.endOfWeek(): LocalDate {
    return this.startOfWeek().plusDays(6)
}
*/

// Funktio muuntaa päivämäärät tekstimuotoon näytettäväksi viikonpäivän lyhyellä nimellä (esim. "Ma", "Ti")
fun formatToDateToDayOfWeek(dateList: List<String>): List<String> {
    val systemLocale = Locale.getDefault()
    var localeForDayOfWeek = Locale("us", "US")
    if (systemLocale.language == "fi") {
        localeForDayOfWeek = Locale("fi", "FI")
    }

    return dateList.map { dateString ->
        val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        if (dateString.matches(datePattern)) {
            // Päivämäärä on muodossa "YYYY-MM-DD"
            val date = LocalDate.parse(dateString)
            // Päivän nimi lyhyellä nimellä (esim. "Ma", "Ti")
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, localeForDayOfWeek)
            // Ota talteen vain kaksi ensimmäistä merkkiä päivän nimestä
            dayOfWeek.take(2)
        } else {
            // Päivämäärä ei ole odotetussa muodossa, oletetaan että se on kuukauden päivä
            dateString
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    onMenuClick: () -> Job,
    goBack: () -> Unit
) {
    val configuration = LocalConfiguration.current

    val viewModel: ProductionViewModel = viewModel()
    // Alusta nykyinen aikaväli tilamuuttuja
    var currentProductionType by remember { mutableStateOf(ProductionTypeInterval.Total) }
    var currentTimeInterval by remember { mutableStateOf(TimeInterval.DAYS) }

    // Määritä nykyisen viikon ensimmäinen päivä
    var currentWeekStartDate by remember { mutableStateOf(LocalDate.now().startOfWeek()) }
    val currentDate by remember { mutableStateOf(LocalDate.now()) }
    var currentWeekEndDate = currentWeekStartDate.plusDays(6)

    // Päivitä kulutustilastot ja lämpötilatilastot haettaessa dataa
    // Reagoi tuotantotyypin tai aikavälin muutoksiin ja nouta tiedot vastaavasti
    LaunchedEffect(
        key1 = currentProductionType,
        key2 = currentTimeInterval,
        key3 = currentWeekStartDate
    ) {
        // Määritä haluamasi päivämäärämuoto
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        // Muotoile currentWeekStartDate merkkijonoksi määritetyssä muodossa
        val formattedDate = currentWeekStartDate.format(formatter)

        when (currentProductionType) {
            ProductionTypeInterval.Total -> {
                viewModel.fetchTotalProductionData(currentTimeInterval, formattedDate)
            }

            ProductionTypeInterval.Wind -> {
                viewModel.fetchWindData(currentTimeInterval, formattedDate)
            }

            ProductionTypeInterval.Solar -> {
                viewModel.fetchSolarData(currentTimeInterval, formattedDate)
            }
        }
    }

    // Refreshauksen käyttöönotto:
    val isLoading by viewModel.isLoading.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
    val refreshDataByTimeInterval: () -> Unit
    var refreshDataByProductionSource = {}

    when (currentTimeInterval) {
        TimeInterval.HOURS -> refreshDataByTimeInterval = {
            viewModel.fetchTotalProductionData(TimeInterval.HOURS, currentWeekStartDate)

            refreshDataByProductionSource = when (currentProductionType) {
                ProductionTypeInterval.Wind -> {
                    {
                        viewModel.fetchWindData(TimeInterval.HOURS, currentWeekStartDate)
                    }
                }

                ProductionTypeInterval.Solar -> {
                    {
                        viewModel.fetchSolarData(TimeInterval.HOURS, currentWeekStartDate)
                    }
                }

                else -> {
                    {
                        viewModel.fetchTotalProductionData(TimeInterval.HOURS, currentWeekStartDate)
                    }
                }
            }
        }

        TimeInterval.WEEKS -> refreshDataByTimeInterval = {
            viewModel.fetchTotalProductionData(TimeInterval.WEEKS, currentWeekStartDate)

            refreshDataByProductionSource = when (currentProductionType) {
                ProductionTypeInterval.Wind -> {
                    {
                        viewModel.fetchWindData(TimeInterval.WEEKS, currentWeekStartDate)
                    }
                }

                ProductionTypeInterval.Solar -> {
                    {
                        viewModel.fetchSolarData(TimeInterval.WEEKS, currentWeekStartDate)
                    }
                }

                else -> {
                    {
                        viewModel.fetchTotalProductionData(TimeInterval.WEEKS, currentWeekStartDate)
                    }
                }
            }
        }

        TimeInterval.MONTHS -> refreshDataByTimeInterval = {
            viewModel.fetchTotalProductionData(TimeInterval.MONTHS, currentWeekStartDate)

            refreshDataByProductionSource = when (currentProductionType) {
                ProductionTypeInterval.Wind -> {
                    {
                        viewModel.fetchWindData(TimeInterval.MONTHS, currentWeekStartDate)
                    }
                }

                ProductionTypeInterval.Solar -> {
                    {
                        viewModel.fetchSolarData(TimeInterval.MONTHS, currentWeekStartDate)
                    }
                }

                else -> {
                    {
                        viewModel.fetchTotalProductionData(TimeInterval.MONTHS, currentWeekStartDate)
                    }
                }
            }
        }

        else -> refreshDataByTimeInterval = {
            viewModel.fetchTotalProductionData(TimeInterval.DAYS, currentWeekStartDate)

            refreshDataByProductionSource = when (currentProductionType) {
                ProductionTypeInterval.Wind -> {
                    {
                        viewModel.fetchWindData(TimeInterval.DAYS, currentWeekStartDate)
                    }
                }

                ProductionTypeInterval.Solar -> {
                    {
                        viewModel.fetchSolarData(TimeInterval.DAYS, currentWeekStartDate)
                    }
                }

                else -> {
                    {
                        viewModel.fetchTotalProductionData(TimeInterval.DAYS, currentWeekStartDate)
                    }
                }
            }
        }
    }

    val refreshAllData = {
        refreshDataByTimeInterval()
        refreshDataByProductionSource()
    }

    // Haetaan localeForMonthAndDay-muuttujaan arvo käyttöjärjestelmän kielen
    // mukaan. Nimetään käyttöliittymässä näkyvä kuukausi ja päivä
    // localeForMonthAndDay-muuttujaan tallennetulla kielellä.
    val systemLocale = Locale.getDefault()
    var localeForTimeIntervals = Locale("us", "US")
    var monthName = currentWeekStartDate.month.getDisplayName(TextStyle.FULL, localeForTimeIntervals)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    var dayName = currentWeekStartDate.dayOfWeek.getDisplayName(
        TextStyle.FULL,
        localeForTimeIntervals
    ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    var currentDayAndMonth = "${currentWeekStartDate.dayOfMonth}/${currentWeekStartDate.monthValue}"
    var currentWeekStartDayAndMonth = "${currentWeekStartDate.dayOfMonth}/${currentWeekStartDate.monthValue}"
    var currentWeekEndDayAndMonth = "${currentWeekEndDate.dayOfMonth}/${currentWeekEndDate.monthValue}"


    if (systemLocale.language == "fi") {
        localeForTimeIntervals = Locale("fi", "FI")
        // Koska kuukausien ja päivien nimet ovat suomenkielisessä
        // käännöksessä partitiivimuodossa, pudotetaan kaksi viimeisintä
        // kirjainta pois.
        monthName = currentWeekStartDate.month.getDisplayName(TextStyle.FULL, localeForTimeIntervals)
            .dropLast(2)
        dayName = currentWeekStartDate.dayOfWeek.getDisplayName(
            TextStyle.FULL,
            localeForTimeIntervals
        ).dropLast(2)
        currentDayAndMonth = "${currentDate.dayOfMonth}.${currentDate.monthValue}."
        currentWeekStartDayAndMonth = "${currentWeekStartDate.dayOfMonth}.${currentWeekStartDate.monthValue}."
        currentWeekEndDayAndMonth = "${currentWeekEndDate.dayOfMonth}.${currentWeekEndDate.monthValue}."
    }

    // Määritä näytön sisältö
    Scaffold(
        topBar = {
            // Yläpalkki
            Surface(shadowElevation = 2.dp) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    // Navigointinappi (takaisin)
                    navigationIcon = {
                        IconButton(onClick = { goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    // Otsikko
                    title = { Text(text = stringResource(R.string.production_title)) },
                    // Toiminnot
                    actions = {
                        IconButton(onClick = { onMenuClick() }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                // Alempi BottomBar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kuukausi-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.MONTHS,
                        onClick = {
                            // Lisää logiikka kuukausidataan siirtymiseen
                            val currentMonthStartDate = LocalDate.now().withDayOfMonth(1)

                            when (currentProductionType) {
                                ProductionTypeInterval.Wind -> {

                                    viewModel.fetchWindData(
                                        TimeInterval.MONTHS,
                                        currentMonthStartDate
                                    )
                                }

                                ProductionTypeInterval.Total -> {
                                    viewModel.fetchTotalProductionData(
                                        TimeInterval.MONTHS,
                                        currentMonthStartDate
                                    )
                                }

                                ProductionTypeInterval.Solar -> {
                                    viewModel.fetchSolarData(
                                        TimeInterval.MONTHS,
                                        currentMonthStartDate
                                    )
                                }
                            }

                            currentTimeInterval = TimeInterval.MONTHS
                        }
                    ) {
                        Text(text = stringResource(R.string.months_text))
                    }
                    // Viikko-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.WEEKS,
                        onClick = {
                            // Hae viikkodata
                            val currentWeekNumber = LocalDate.now()
                                .get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                            val currentYear = LocalDate.now().year

                            val firstDayOfWeek = LocalDate.ofYearDay(currentYear, 1)
                                .with(
                                    WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(),
                                    currentWeekNumber.toLong()
                                )
                                .with(DayOfWeek.MONDAY)

                            currentWeekStartDate = firstDayOfWeek
                            currentWeekEndDate = firstDayOfWeek.plusDays(6)

                            when (currentProductionType) {
                                ProductionTypeInterval.Wind -> {
                                    viewModel.fetchWindData(
                                        TimeInterval.WEEKS,
                                        currentWeekStartDate
                                    )
                                }

                                ProductionTypeInterval.Total -> {
                                    viewModel.fetchTotalProductionData(
                                        TimeInterval.WEEKS,
                                        currentWeekStartDate
                                    )
                                }

                                ProductionTypeInterval.Solar -> {
                                    viewModel.fetchSolarData(
                                        TimeInterval.WEEKS,
                                        currentWeekStartDate
                                    )
                                }
                            }

                            currentTimeInterval = TimeInterval.WEEKS

                        }
                    ) {
                        Text(text = stringResource(R.string.weeks_text))
                    }

                    // Päivä-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.DAYS,
                        onClick = {
                            // Hae päivädata
                            currentWeekStartDate = LocalDate.now().startOfWeek()

                            when (currentProductionType) {
                                ProductionTypeInterval.Wind -> {
                                    viewModel.fetchWindData(TimeInterval.DAYS, currentWeekStartDate)
                                }

                                ProductionTypeInterval.Total -> {
                                    viewModel.fetchTotalProductionData(
                                        TimeInterval.DAYS,
                                        currentWeekStartDate
                                    )
                                }

                                ProductionTypeInterval.Solar -> {
                                    viewModel.fetchSolarData(
                                        TimeInterval.DAYS,
                                        currentWeekStartDate
                                    )
                                }
                            }

                            currentTimeInterval = TimeInterval.DAYS
                        }
                    ) {
                        Text(text = stringResource(R.string.days_text))
                    }

                    // Tunti-nappi
                    Button(
                        enabled = currentTimeInterval != TimeInterval.HOURS,
                        onClick = {
                            // Hae tuntidata
                            currentWeekStartDate = LocalDate.now()

                            when (currentProductionType) {
                                ProductionTypeInterval.Wind -> {
                                    viewModel.fetchWindData(
                                        TimeInterval.HOURS,
                                        currentWeekStartDate
                                    )
                                }

                                ProductionTypeInterval.Total -> {
                                    viewModel.fetchTotalProductionData(
                                        TimeInterval.HOURS,
                                        currentWeekStartDate
                                    )
                                }

                                ProductionTypeInterval.Solar -> {
                                    viewModel.fetchSolarData(
                                        TimeInterval.HOURS,
                                        currentWeekStartDate
                                    )
                                }
                            }

                            currentTimeInterval = TimeInterval.HOURS
                        }
                    ) {
                        Text(text = stringResource(R.string.hours_text))
                    }
                }
            }
        }
    ) { innerPaddings ->
        // Näytön sisältö
        Box(
            modifier = Modifier
                .padding(innerPaddings)
                .fillMaxSize()
        ) {

            when {
                // Latauspalkki
                viewModel.productionChartState.value.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                // HORISONTAALINEN
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ->
                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = { refreshAllData() },
                        indicator = { state, refreshTrigger ->
                            SwipeRefreshIndicator(
                                state = state,
                                refreshTriggerDistance = refreshTrigger,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )
                    {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                ProductionChart(
                                    productionStatsData = viewModel.productionStatsData,
                                    currentProductionType = currentProductionType,
                                    isLandscape = true
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Nuoli vasemmalle
                                    IconButton(
                                        onClick = {
                                            when (currentTimeInterval) {
                                                TimeInterval.DAYS -> {
                                                    // Siirry edellisen viikon alkuun
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.minusWeeks(1)
                                                            .startOfWeek()

                                                }

                                                TimeInterval.HOURS -> {
                                                    // Siirry edellisen päivän alkuun
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.minusDays(1)

                                                }

                                                TimeInterval.WEEKS -> {
                                                    // Siirry edellisen kuukauden alkuun
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.minusMonths(1)
                                                            .startOfWeek()

                                                }

                                                TimeInterval.MONTHS -> {
                                                    // Siirry edellisen vuoden alkuun
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.minusYears(1)
                                                }

                                                TimeInterval.MAIN -> TODO()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }

                                    // Näytä päivämäärä
                                    Text(
                                        text = when (currentTimeInterval) {
                                            TimeInterval.DAYS -> {
                                                "$currentWeekStartDayAndMonth – $currentWeekEndDayAndMonth"
                                            }

                                            TimeInterval.HOURS -> {
                                                "$dayName ($currentDayAndMonth)"
                                            }

                                            TimeInterval.WEEKS -> {
                                                "$monthName ${currentWeekStartDate.year}"
                                            }

                                            TimeInterval.MONTHS -> {
                                                "${currentWeekStartDate.year}"
                                            }

                                            TimeInterval.MAIN -> TODO()
                                        },
                                        fontSize = 30.sp
                                    )

                                    // Nuoli oikealle
                                    IconButton(
                                        onClick = {
                                            // Siirry seuraavan aikavälin alkuun
                                            when (currentTimeInterval) {
                                                TimeInterval.DAYS -> {
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.plusWeeks(1)
                                                            .startOfWeek()
                                                }

                                                TimeInterval.HOURS -> {
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.plusDays(1)
                                                }

                                                TimeInterval.WEEKS -> {
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.plusMonths(1)
                                                            .startOfWeek()
                                                }

                                                TimeInterval.MONTHS -> {
                                                    currentWeekStartDate =
                                                        currentWeekStartDate.plusYears(1)
                                                }

                                                TimeInterval.MAIN -> TODO()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Display production summary text
                                        Text(
                                            modifier = Modifier
                                                .padding(vertical = 16.dp)
                                                .weight(1f), // Use a larger weight for the label to push the value right
                                            text = stringResource(R.string.total_pro_text) + ":",
                                            fontSize = 30.sp
                                        )

                                        // Display production summary data
                                        Text(
                                            modifier = Modifier
                                                .padding(vertical = 16.dp)
                                                .weight(1f), // Smaller weight for the data to align to the end
                                            text = "${
                                                String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    when (currentProductionType) {
                                                        ProductionTypeInterval.Wind -> {
                                                            viewModel.windStatsData?.values?.sum() ?: 0f
                                                        }

                                                        ProductionTypeInterval.Total -> {
                                                            viewModel.productionStatsData?.values?.sum() ?: 0f
                                                        }

                                                        ProductionTypeInterval.Solar -> {
                                                            viewModel.solarStatsData?.values?.sum() ?: 0f
                                                        }
                                                    }
                                                )
                                            } kwh",
                                            fontSize = 30.sp,
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Display average production text
                                        Text(
                                            modifier = Modifier
                                                .padding(vertical = 16.dp)
                                                .weight(1f), // Consistent weight for label
                                            text = stringResource(R.string.avg_pro_text) + ":",
                                            fontSize = 30.sp
                                        )

                                        // Display average production data
                                        Text(
                                            modifier = Modifier
                                                .padding(vertical = 16.dp)
                                                .weight(1f), // Consistent weight for data
                                            text = "${
                                                String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    when (currentProductionType) {
                                                        ProductionTypeInterval.Wind -> {
                                                            viewModel.windStatsData?.values?.average() ?: 0f
                                                        }

                                                        ProductionTypeInterval.Total -> {
                                                            viewModel.productionStatsData?.values?.average() ?: 0f
                                                        }

                                                        ProductionTypeInterval.Solar -> {
                                                            viewModel.solarStatsData?.values?.average() ?: 0f
                                                        }
                                                    }
                                                )
                                            } kwh",
                                            fontSize = 30.sp,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    // Ylempi BottomBar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Solar-nappi
                                        ElevatedButton(
                                            enabled = currentProductionType != ProductionTypeInterval.Solar,
                                            onClick = {
                                                // Lisää logiikka aurinko dataan siirtymiseen
                                                currentProductionType = ProductionTypeInterval.Solar
                                                viewModel.fetchData(
                                                    TimeInterval.DAYS,
                                                    currentWeekStartDate
                                                )
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Brightness5,
                                                contentDescription = "Solar"
                                            )
                                        }

                                        // Wind-nappi
                                        ElevatedButton(
                                            enabled = currentProductionType != ProductionTypeInterval.Wind,
                                            onClick = {
                                                // Lisää logiikka tuuli dataan siirtymiseen
                                                currentProductionType = ProductionTypeInterval.Wind
                                                viewModel.fetchData(
                                                    TimeInterval.DAYS,
                                                    currentWeekStartDate
                                                )

                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Air,
                                                contentDescription = "Wind"
                                            )
                                        }
                                        // Total Production-nappi
                                        ElevatedButton(
                                            enabled = currentProductionType != ProductionTypeInterval.Total,
                                            onClick = {
                                                // Lisää logiikka total production dataan siirtymiseen
                                                currentProductionType = ProductionTypeInterval.Total
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.BatteryChargingFull,
                                                contentDescription = "Total Production"
                                            )
                                        }
                                    }

                                } // end of column

                            }
                        }

                    }

                // VERTIKAALINEN
                else -> SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { refreshAllData() },
                    indicator = { state, refreshTrigger ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = refreshTrigger,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        ProductionChart(
                            viewModel.productionStatsData,
                            currentProductionType = currentProductionType,
                            isLandscape = false
                        )
                        Spacer(modifier = Modifier.height(15.dp))

                        // Piirrä nuolinapit ja niiden välissä oleva aikaväli
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nuoli vasemmalle
                            IconButton(
                                onClick = {
                                    when (currentTimeInterval) {
                                        TimeInterval.DAYS -> {
                                            // Siirry edellisen viikon alkuun
                                            currentWeekStartDate =
                                                currentWeekStartDate.minusWeeks(1).startOfWeek()

                                        }

                                        TimeInterval.HOURS -> {
                                            // Siirry edellisen päivän alkuun
                                            currentWeekStartDate = currentWeekStartDate.minusDays(1)

                                        }

                                        TimeInterval.WEEKS -> {
                                            // Siirry edellisen kuukauden alkuun
                                            currentWeekStartDate =
                                                currentWeekStartDate.minusMonths(1).startOfWeek()

                                        }

                                        TimeInterval.MONTHS -> {
                                            // Siirry edellisen vuoden alkuun
                                            currentWeekStartDate =
                                                currentWeekStartDate.minusYears(1)
                                        }

                                        TimeInterval.MAIN -> TODO()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.weight(0.5f))

                            // Näytä päivämäärä
                            Text(
                                text = when (currentTimeInterval) {
                                    TimeInterval.DAYS -> {
                                        "$currentWeekStartDayAndMonth – $currentWeekEndDayAndMonth"
                                    }

                                    TimeInterval.HOURS -> {
                                        "$dayName ($currentDayAndMonth)"
                                    }

                                    TimeInterval.WEEKS -> {
                                        "$monthName ${currentWeekStartDate.year}"
                                    }

                                    TimeInterval.MONTHS -> {
                                        "${currentWeekStartDate.year}"
                                    }

                                    TimeInterval.MAIN -> TODO()
                                },
                                fontSize = 30.sp
                            )
                            Spacer(modifier = Modifier.weight(0.5f))

                            // Nuoli oikealle
                            IconButton(
                                onClick = {
                                    // Siirry seuraavan aikavälin alkuun
                                    when (currentTimeInterval) {
                                        TimeInterval.DAYS -> {
                                            currentWeekStartDate =
                                                currentWeekStartDate.plusWeeks(1).startOfWeek()
                                        }

                                        TimeInterval.HOURS -> {
                                            currentWeekStartDate = currentWeekStartDate.plusDays(1)
                                        }

                                        TimeInterval.WEEKS -> {
                                            currentWeekStartDate =
                                                currentWeekStartDate.plusMonths(1).startOfWeek()
                                        }

                                        TimeInterval.MONTHS -> {
                                            currentWeekStartDate = currentWeekStartDate.plusYears(1)
                                        }

                                        TimeInterval.MAIN -> TODO()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.4f))

                        // Summary data
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Row(
                                    Modifier.padding(30.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Values text
                                    Column {
                                        // Display total production text
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = stringResource(R.string.total_pro_text) + ":",
                                            fontSize = 24.sp
                                        )
                                        // Display average production text
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = stringResource(R.string.avg_pro_text) + ":",
                                            fontSize = 24.sp
                                        )
                                    }
                                    Spacer(Modifier.width(35.dp))

                                    // Values
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        // Display total production data
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = String.format(
                                                Locale.US,
                                                "%.2f",
                                                when (currentProductionType) {
                                                    ProductionTypeInterval.Wind -> {
                                                        viewModel.windStatsData?.values?.sum() ?: 0f
                                                    }

                                                    ProductionTypeInterval.Total -> {
                                                        viewModel.productionStatsData?.values?.sum()
                                                        ?: 0f
                                                    }

                                                    ProductionTypeInterval.Solar -> {
                                                        viewModel.solarStatsData?.values?.sum()
                                                        ?: 0f
                                                    }
                                                }
                                            ),
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.inverseSurface
                                        )
                                        // Display average production data
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = String.format(
                                                Locale.US,
                                                "%.2f",
                                                when (currentProductionType) {
                                                    ProductionTypeInterval.Wind -> {
                                                        viewModel.windStatsData?.values?.average()
                                                        ?: 0f
                                                    }

                                                    ProductionTypeInterval.Total -> {
                                                        viewModel.productionAvgStatsData
                                                        ?: 0f
                                                    }

                                                    ProductionTypeInterval.Solar -> {
                                                        viewModel.solarStatsData?.values?.average()
                                                        ?: 0f
                                                    }
                                                }
                                            ),
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.inverseSurface
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))

                                    // Units
                                    Column {
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = "kWh",
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = "kWh",
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.6f))

                        // Upper BottomBar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Solar-nappi
                            ElevatedButton(
                                enabled = currentProductionType != ProductionTypeInterval.Solar,
                                onClick = {
                                    // Lisää logiikka aurinko dataan siirtymiseen
                                    currentProductionType = ProductionTypeInterval.Solar
                                    viewModel.fetchData(
                                        TimeInterval.DAYS,
                                        currentWeekStartDate
                                    )
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Brightness5,
                                    contentDescription = "Solar"
                                )
                            }

                            // Wind-nappi
                            ElevatedButton(
                                enabled = currentProductionType != ProductionTypeInterval.Wind,
                                onClick = {
                                    // Lisää logiikka tuuli dataan siirtymiseen
                                    currentProductionType = ProductionTypeInterval.Wind
                                    viewModel.fetchData(
                                        TimeInterval.DAYS,
                                        currentWeekStartDate
                                    )

                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Air,
                                    contentDescription = "Wind"
                                )
                            }
                            // Total Production-nappi
                            ElevatedButton(
                                enabled = currentProductionType != ProductionTypeInterval.Total,
                                onClick = {
                                    // Lisää logiikka total production dataan siirtymiseen
                                    currentProductionType = ProductionTypeInterval.Total
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BatteryChargingFull,
                                    contentDescription = "Total Production"
                                )
                            }
                        } // End of upper bottombar
                    } // end of else (vertical layout)
                }
            }
        }
    }
}

