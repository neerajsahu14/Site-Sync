package com.a1.sitesync.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a1.sitesync.data.database.model.Survey
import com.a1.sitesync.data.database.model.SurveyWithPhotos
import com.a1.sitesync.data.repository.SiteSyncRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class SiteSyncViewModel(
    private val repository: SiteSyncRepository
) : ViewModel() {

    private val _surveys = MutableStateFlow<List<SurveyWithPhotos>>(emptyList())
    val surveys: StateFlow<List<SurveyWithPhotos>> = _surveys.asStateFlow()

    private var currentPage = 0
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private var isLastPage = false

    companion object {
        private const val PAGE_SIZE = 10
    }

    init {
        loadMoreSurveys()
    }

    fun loadMoreSurveys() {
        if (_isLoading.value || isLastPage) return

        viewModelScope.launch {
            _isLoading.value = true
            currentPage++
            val newSurveys = repository.getPagedSurveys(page = currentPage, pageSize = PAGE_SIZE)
            if (newSurveys.size < PAGE_SIZE) {
                isLastPage = true
            }
            _surveys.value = _surveys.value + newSurveys
            _isLoading.value = false
        }
    }

    fun getSurvey(id: String) = repository.getSurveyById(id)

    suspend fun createSurvey(
        surveyorId: String,
        clientName: String,
        siteAddress: String?,
        gateType: String,
        dimensions: com.a1.sitesync.data.database.model.Dimensions,
        provisions: com.a1.sitesync.data.database.model.Provisions,
        openingDirection: String?,
        recommendedGate: String?,
        photoPaths: List<String>,
        latitude: Double?,
        longitude: Double?
    ): String {
        return repository.createNewSurvey(
            surveyorId, clientName, siteAddress, gateType, dimensions, provisions,
            openingDirection, recommendedGate, photoPaths, latitude, longitude
        )
    }

    fun updateSurvey(survey: Survey) = viewModelScope.launch {
        repository.updateSurvey(survey)
    }

    fun deleteSurvey(survey: Survey) = viewModelScope.launch {
        repository.deleteSurvey(survey)
    }

    fun deletePhotoById(photoId: String) = viewModelScope.launch {
        repository.deletePhotoById(photoId)
    }

    fun syncData() = viewModelScope.launch {
        repository.performDataSync()
    }

    fun syncSurveyById(surveyId: String) = viewModelScope.launch {
        repository.syncSurveyById(surveyId)
    }

    private val _reportFilePath = MutableStateFlow<String?>(null)
    val reportFilePath: StateFlow<String?> = _reportFilePath

    fun generateReport(context: Context, surveyId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val surveyWithPhotos = repository.getSurveyById(surveyId).first()
            val survey = surveyWithPhotos.survey
            val doc = PDDocument()

            val detailsPage = PDPage(PDRectangle.A4)
            doc.addPage(detailsPage)
            val contentStream = PDPageContentStream(doc, detailsPage)

            var yPosition = 750f

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
            contentStream.newLineAtOffset(150f, yPosition)
            contentStream.showText("Site Survey Report")
            contentStream.endText()
            yPosition -= 50f

            fun addTextLine(label: String, value: String?) {
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 12f)
                contentStream.newLineAtOffset(50f, yPosition)
                contentStream.showText("$label: ${value ?: "N/A"}")
                contentStream.endText()
                yPosition -= 20f
            }

            addTextLine("Client Name", survey.clientName)
            addTextLine("Site Address", survey.siteAddress)
            addTextLine("Gate Type", survey.gateType)
            addTextLine("Clear Opening Width", "${survey.dimensions.clearOpeningWidth} m")
            addTextLine("Required Height", "${survey.dimensions.requiredHeight} m")
            survey.dimensions.parkingSpaceLength?.let { addTextLine("Parking Space Length", "$it m") }
            survey.dimensions.openingAngleLeaf?.let { addTextLine("Opening Angle", "$it degrees") }
            addTextLine("Provision for Cabling", if (survey.provisions.hasCabling) "Yes" else "No")
            addTextLine("Provision for Storage", if (survey.provisions.hasStorage) "Yes" else "No")
            addTextLine("Latitude", survey.latitude?.toString())
            addTextLine("Longitude", survey.longitude?.toString())

            contentStream.close()

            surveyWithPhotos.photos.forEach { photo ->
                val imgFile = File(photo.localFilePath)
                if (imgFile.exists()) {
                    val pdImage = PDImageXObject.createFromFileByContent(imgFile, doc)
                    val page = PDPage(PDRectangle.A4)
                    doc.addPage(page)
                    val imageContentStream = PDPageContentStream(doc, page)
                    val scale = 0.4f
                    val imgWidth = pdImage.width * scale
                    val imgHeight = pdImage.height * scale
                    val x = (page.mediaBox.width - imgWidth) / 2
                    val y = (page.mediaBox.height - imgHeight) / 2
                    imageContentStream.drawImage(pdImage, x, y, imgWidth, imgHeight)
                    imageContentStream.close()
                }
            }

            val reportFile = File(context.externalCacheDir, "${surveyId}_report.pdf")
            doc.save(reportFile)
            doc.close()
            _reportFilePath.value = reportFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addPhotoToSurvey(surveyId: String, localFilePath: String, isSuperimposed: Boolean = false) = viewModelScope.launch {
        repository.addPhotoToSurvey(surveyId, localFilePath, isSuperimposed)
    }
}