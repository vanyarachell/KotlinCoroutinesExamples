/*
 *  Copyright 2017 Andrea Bresolin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package andreabresolin.kotlincoroutinesexamples.home.view

import andreabresolin.kotlincoroutinesexamples.R
import andreabresolin.kotlincoroutinesexamples.app.App
import andreabresolin.kotlincoroutinesexamples.home.di.HomeModule
import andreabresolin.kotlincoroutinesexamples.home.presenter.HomePresenter
import andreabresolin.kotlincoroutinesexamples.home.view.HomeView.WeatherRetrievalErrorDialogResponse
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class HomeActivity : AppCompatActivity(), HomeView {

    @Inject
    internal lateinit var presenter: HomePresenter

    private lateinit var citiesTextViews: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        injectDependencies()
        setupListeners()

        citiesTextViews = listOf(
                currentWeatherCity1Text,
                currentWeatherCity2Text,
                currentWeatherCity3Text)
    }

    private fun injectDependencies() {
        App.get()
                .getAppComponent()
                ?.plus(HomeModule(this))
                ?.inject(this)
    }

    private fun setupListeners() {
        getCurrentWeatherSequentialButton.setOnClickListener { onGetCurrentWeatherSequentialButtonClick() }
        getCurrentWeatherParallelButton.setOnClickListener { onGetCurrentWeatherParallelButtonClick() }
        getAverageTemperatureButton.setOnClickListener { onGetAverageTemperatureButtonClick() }
        getCurrentWeatherWithRetryButton.setOnClickListener { onGetCurrentWeatherWithRetryButtonClick() }
    }

    override fun onStop() {
        presenter.cleanup()
        super.onStop()
    }

    private fun onGetCurrentWeatherSequentialButtonClick() {
        presenter.getCurrentWeatherSequential()
    }

    private fun onGetCurrentWeatherParallelButtonClick() {
        presenter.getCurrentWeatherParallel()
    }

    private fun onGetAverageTemperatureButtonClick() {
        presenter.getAverageTemperatureInCities()
    }

    private fun onGetCurrentWeatherWithRetryButtonClick() {
        presenter.getCurrentWeatherForCityWithRetry()
    }

    override fun clearAllCities() {
        citiesTextViews.forEach { it.text = "" }
    }

    override fun displayInProgressForCity(cityIndex: Int) {
        citiesTextViews[cityIndex].text = getString(R.string.retrieval_in_progress)
    }

    override fun displayCanceledForCity(cityIndex: Int) {
        citiesTextViews[cityIndex].text = getString(R.string.retrieval_canceled)
    }

    override fun displayWeatherForCity(cityIndex: Int, cityName: String, description: String, temperature: Double) {
        citiesTextViews[cityIndex].text = getString(R.string.retrieval_result, cityName, description, temperature)
    }

    override fun displayAverageTemperature(averageTemperature: Double) {
        AlertDialog.Builder(this)
                .setTitle(R.string.average_temperature_dialog_title)
                .setMessage(getString(R.string.average_temperature_dialog_message, averageTemperature))
                .setPositiveButton(R.string.ok_dialog_button, {
                    dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                })
                .create()
                .show()
    }

    override fun displayWeatherRetrievalErrorDialog(place: String) {
        AlertDialog.Builder(this)
                .setTitle(R.string.retrieval_error_dialog_title)
                .setMessage(getString(R.string.retrieval_error_dialog_message_with_place, place))
                .setPositiveButton(R.string.ok_dialog_button, {
                    dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                })
                .create()
                .show()
    }

    override suspend fun displayWeatherRetrievalErrorDialogWithRetry(place: String): WeatherRetrievalErrorDialogResponse {
        lateinit var result: Continuation<WeatherRetrievalErrorDialogResponse>

        AlertDialog.Builder(this)
                .setTitle(R.string.retrieval_error_dialog_title)
                .setMessage(getString(R.string.retrieval_error_dialog_message_with_retry, place))
                .setPositiveButton(R.string.retry_dialog_button, {
                    dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    result.resume(WeatherRetrievalErrorDialogResponse.RETRY)
                })
                .setNegativeButton(R.string.cancel_dialog_button, {
                    dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    result.resume(WeatherRetrievalErrorDialogResponse.CANCEL)
                })
                .setOnCancelListener {
                    result.resume(WeatherRetrievalErrorDialogResponse.CANCEL)
                }
                .create()
                .show()

        return suspendCoroutine { continuation -> result = continuation }
    }

    override fun displayWeatherRetrievalGenericError() {
        AlertDialog.Builder(this)
                .setTitle(R.string.retrieval_error_dialog_title)
                .setMessage(R.string.retrieval_error_dialog_message)
                .setPositiveButton(R.string.ok_dialog_button, {
                    dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                })
                .create()
                .show()
    }
}