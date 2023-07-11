package no.seime.openhab.binding.esphome.internal.message;

import java.util.Collections;
import java.util.stream.Collectors;

import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.types.Command;

import io.esphome.api.ClimateCommandRequest;
import io.esphome.api.ClimateStateResponse;
import io.esphome.api.ListEntitiesClimateResponse;
import no.seime.openhab.binding.esphome.internal.BindingConstants;
import no.seime.openhab.binding.esphome.internal.EnumHelper;
import no.seime.openhab.binding.esphome.internal.comm.ProtocolAPIError;
import no.seime.openhab.binding.esphome.internal.handler.ESPHomeHandler;

public class ClimateMessageHandler extends AbstractMessageHandler<ListEntitiesClimateResponse, ClimateStateResponse> {

    public static final String CHANNEL_TARGET_TEMPERATURE = "target_temperature";
    public static final String CHANNEL_FAN_MODE = "fan_mode";
    public static final String CHANNEL_CUSTOM_FAN_MODE = "custom_fan_mode";
    public static final String CHANNEL_PRESET = "preset";
    public static final String CHANNEL_CUSTOM_PRESET = "custom_preset";
    public static final String CHANNEL_SWING_MODE = "swing_mode";
    public static final String CHANNEL_CURRENT_TEMPERATURE = "current_temperature";
    public static final String CHANNEL_MODE = "mode";

    public ClimateMessageHandler(ESPHomeHandler handler) {
        super(handler);
    }

    @Override
    public void handleCommand(Channel channel, Command command, int key) throws ProtocolAPIError {
        ClimateCommandRequest.Builder builder = ClimateCommandRequest.newBuilder().setKey(key);
        String subCommand = (String) channel.getConfiguration().get(BindingConstants.COMMAND_FIELD);
        switch (subCommand) {
            case CHANNEL_MODE:
                builder.setMode(EnumHelper.toClimateMode(command.toString())).setHasMode(true);
                break;
            case CHANNEL_TARGET_TEMPERATURE:
                if (command instanceof QuantityType<?>) {
                    QuantityType<?> qt = (QuantityType<?>) command;
                    builder.setTargetTemperature(qt.floatValue());
                } else if (command instanceof DecimalType) {
                    DecimalType dc = (DecimalType) command;
                    builder.setTargetTemperature(dc.floatValue());
                }

                builder.setHasTargetTemperature(true);
                break;
            case CHANNEL_FAN_MODE:
                builder.setFanMode(EnumHelper.toFanMode(command.toString())).setHasFanMode(true);
                break;
            case CHANNEL_CUSTOM_FAN_MODE:
                builder.setCustomFanMode(command.toString()).setHasCustomFanMode(true);
                break;
            case CHANNEL_PRESET:
                builder.setPreset(EnumHelper.toClimatePreset(command.toString())).setHasPreset(true);
                break;
            case CHANNEL_CUSTOM_PRESET:
                builder.setCustomPreset(command.toString()).setHasCustomPreset(true);
                break;
            case CHANNEL_SWING_MODE:
                builder.setSwingMode(EnumHelper.toClimateSwingMode(command.toString())).setHasSwingMode(true);
                break;
        }

        handler.sendMessage(builder.build());
    }

    @Override
    public void buildChannels(ListEntitiesClimateResponse rsp) {

        ChannelType channelTypeTargetTemperature = addChannelType(
                rsp.getObjectId() + "_" + BindingConstants.CHANNEL_NAME_TARGET_TEMPERATURE, "Target temperature",
                "Number:Temperature", Collections.emptyList(), "%.1f", null);

        Channel channelTargetTemperature = ChannelBuilder
                .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_TARGET_TEMPERATURE))
                .withLabel("Target temperature").withKind(ChannelKind.STATE)
                .withType(channelTypeTargetTemperature.getUID())
                .withConfiguration(configuration(rsp.getKey(), CHANNEL_TARGET_TEMPERATURE, "Climate")).build();
        super.registerChannel(channelTargetTemperature, channelTypeTargetTemperature);

        if (rsp.getSupportsCurrentTemperature()) {
            ChannelType channelType = addChannelType(
                    rsp.getObjectId() + "_" + BindingConstants.CHANNEL_NAME_CURRENT_TEMPERATURE, "Current temperature",
                    "Number:Temperature", Collections.emptyList(), "%.1f", null);

            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(),
                            BindingConstants.CHANNEL_NAME_CURRENT_TEMPERATURE))
                    .withLabel("Current temperature").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_CURRENT_TEMPERATURE, null)).build();
            super.registerChannel(channel, channelType);
        }

        if (rsp.getSupportedModesCount() > 0) {
            ChannelType channelType = addChannelType(rsp.getObjectId() + "_" + BindingConstants.CHANNEL_NAME_MODE,
                    "Mode", "String", rsp.getSupportedModesList().stream().map(val -> EnumHelper.stripEnumPrefix(val))
                            .collect(Collectors.toList()),
                    "%s", null);

            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_MODE))
                    .withLabel("Mode").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_MODE, "Climate")).build();
            super.registerChannel(channel, channelType);
        }
        if (rsp.getSupportedFanModesCount() > 0) {
            ChannelType channelType = addChannelType(
                    BindingConstants.CHANNEL_NAME_FAN_MODE, "Fan Mode", "String", rsp.getSupportedFanModesList()
                            .stream().map(val -> EnumHelper.stripEnumPrefix(val)).collect(Collectors.toList()),
                    "%s", null);

            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_FAN_MODE))
                    .withLabel("Fan Mode").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_FAN_MODE, "Climate")).build();
            super.registerChannel(channel, channelType);
        }
        if (rsp.getSupportedCustomFanModesCount() > 0) {
            ChannelType channelType = addChannelType(BindingConstants.CHANNEL_NAME_CUSTOM_FAN_MODE, "Custom Fan Mode",
                    "String", rsp.getSupportedCustomFanModesList().stream().collect(Collectors.toList()), "%s", null);

            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_CUSTOM_FAN_MODE))
                    .withLabel("Custom Fan Mode").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_CUSTOM_FAN_MODE, "Climate")).build();
            super.registerChannel(channel, channelType);
        }
        if (rsp.getSupportedPresetsCount() > 0) {
            ChannelType channelType = addChannelType(rsp.getObjectId() + "_" + BindingConstants.CHANNEL_NAME_PRESET,
                    "Preset", "String", rsp.getSupportedPresetsList().stream()
                            .map(val -> EnumHelper.stripEnumPrefix(val)).collect(Collectors.toList()),
                    "%s", null);
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_PRESET))
                    .withLabel("Preset").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_PRESET, "Climate")).build();
            super.registerChannel(channel, channelType);
        }
        if (rsp.getSupportedCustomPresetsCount() > 0) {
            ChannelType channelType = addChannelType(
                    rsp.getObjectId() + "_" + BindingConstants.CHANNEL_NAME_CUSTOM_PRESET, "Custom Preset", "String",
                    rsp.getSupportedCustomPresetsList().stream().collect(Collectors.toList()), "%s", null);
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_CUSTOM_PRESET))
                    .withLabel("Custom Preset").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_CUSTOM_PRESET, "Climate")).build();
            super.registerChannel(channel, channelType);
        }
        if (rsp.getSupportedSwingModesCount() > 0) {
            ChannelType channelType = addChannelType(rsp.getObjectId() + "_" + BindingConstants.CHANNEL_NAME_SWING_MODE,
                    "Swing Mode", "String", rsp.getSupportedSwingModesList().stream()
                            .map(val -> EnumHelper.stripEnumPrefix(val)).collect(Collectors.toList()),
                    "%s", null);
            Channel channel = ChannelBuilder
                    .create(new ChannelUID(handler.getThing().getUID(), BindingConstants.CHANNEL_NAME_SWING_MODE))
                    .withLabel("Swing Mode").withKind(ChannelKind.STATE).withType(channelType.getUID())
                    .withConfiguration(configuration(rsp.getKey(), CHANNEL_SWING_MODE, "Climate")).build();
            super.registerChannel(channel, channelType);
        }
    }

    @Override
    public void handleState(ClimateStateResponse rsp) {
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_TARGET_TEMPERATURE).ifPresent(channel -> handler
                .updateState(channel.getUID(), new QuantityType<>(rsp.getTargetTemperature(), SIUnits.CELSIUS)));
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_CURRENT_TEMPERATURE).ifPresent(channel -> handler
                .updateState(channel.getUID(), new QuantityType<>(rsp.getCurrentTemperature(), SIUnits.CELSIUS)));
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_MODE).ifPresent(channel -> handler.updateState(channel.getUID(),
                new StringType(EnumHelper.stripEnumPrefix(rsp.getMode()))));
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_FAN_MODE).ifPresent(channel -> handler
                .updateState(channel.getUID(), new StringType(EnumHelper.stripEnumPrefix(rsp.getFanMode()))));
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_CUSTOM_FAN_MODE)
                .ifPresent(channel -> handler.updateState(channel.getUID(), new StringType(rsp.getCustomFanMode())));

        findChannelByKeyAndField(rsp.getKey(), CHANNEL_PRESET).ifPresent(channel -> handler
                .updateState(channel.getUID(), new StringType(EnumHelper.stripEnumPrefix(rsp.getPreset()))));
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_CUSTOM_PRESET)
                .ifPresent(channel -> handler.updateState(channel.getUID(), new StringType(rsp.getCustomPreset())));
        findChannelByKeyAndField(rsp.getKey(), CHANNEL_SWING_MODE).ifPresent(channel -> handler
                .updateState(channel.getUID(), new StringType(EnumHelper.stripEnumPrefix(rsp.getSwingMode()))));
    }
}
