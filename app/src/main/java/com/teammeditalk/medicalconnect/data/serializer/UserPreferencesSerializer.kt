package com.teammeditalk.medicalconnect.data.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.teammeditalk.medicalconnect.UserInfo
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserInfo> {
    override val defaultValue: UserInfo = UserInfo.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserInfo {
        try {
            return UserInfo.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("cannot read proto ", e)
        }
    }

    override suspend fun writeTo(
        t: UserInfo,
        output: OutputStream,
    ) = t.writeTo(output)
}
