// SPDX-FileCopyrightText: Copyright 2023 Mark Rotteveel
// SPDX-License-Identifier: Apache-2.0
package nl.lawinegevaar.exttablegen.type;

import java.time.temporal.TemporalAccessor;

public sealed interface FbDatetimeType<T extends TemporalAccessor> extends FbDatatype<T>
        permits FbDate, FbTime, FbTimestamp {
}
