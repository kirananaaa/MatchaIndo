package com.example.machaindo.data.model

import com.google.gson.annotations.SerializedName

data class AbsensiRequest(
    val latitude: Double,
    val longitude: Double
)

data class AbsensiResponse(
    val message: String?,
    val data: AbsensiStatus
)

// Dipakai untuk membaca pesan dari respons error (4xx)
data class ApiError(
    val message: String?
)

data class AbsensiStatus(
    val tanggal: String,
    @SerializedName("sudah_masuk") val sudahMasuk: Boolean,
    @SerializedName("sudah_pulang") val sudahPulang: Boolean,
    @SerializedName("jam_masuk") val jamMasuk: String?,
    @SerializedName("jam_pulang") val jamPulang: String?,
    val status: String?,
    @SerializedName("menit_telat") val menitTelat: Int,
    val shift: ShiftInfo?,
    val cabang: CabangLokasi?
)

data class ShiftInfo(
    @SerializedName("nama_shift") val namaShift: String,
    @SerializedName("jam_mulai") val jamMulai: String,
    @SerializedName("jam_selesai") val jamSelesai: String
)

data class CabangLokasi(
    @SerializedName("nama_cabang") val namaCabang: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("radius_meter") val radiusMeter: Int
)
