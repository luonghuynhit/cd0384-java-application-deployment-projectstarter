package com.udacity.catpoint.security;

import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTest {

    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityService = new SecurityService(securityRepository);
    }

    @Test
    void ifAlarmArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifAlarmArmedAndSensorActivated_changeStatusToAlarmIfAlreadyPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifPendingAlarmAndAllSensorsInactive_returnToNoAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(
                new Sensor("Front Door", SensorType.DOOR),
                new Sensor("Back Door", SensorType.DOOR)
        ));

        securityService.changeSensorActivationStatus(new Sensor("Front Door", SensorType.DOOR), false);
        securityService.changeSensorActivationStatus(new Sensor("Back Door", SensorType.DOOR), false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifAlarmActive_changeSensorShouldNotAffectAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifSensorActivatedWhileActiveAndSystemPending_changeToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifSensorDeactivatedWhileInactive_makeNoChangesToAlarmState() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifImageServiceIdentifiesCatWhileArmedHome_changeToAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifImageServiceIdentifiesNoCatAndNoSensorsActive_changeToNoAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(
                new Sensor("Front Door", SensorType.DOOR),
                new Sensor("Back Door", SensorType.DOOR)
        ));

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifSystemDisarmed_changeStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetAllSensorsToInactive(ArmingStatus armingStatus) {
        when(securityRepository.getSensors()).thenReturn(Set.of(
                new Sensor("Front Door", SensorType.DOOR),
                new Sensor("Back Door", SensorType.DOOR)
        ));

        securityService.setArmingStatus(armingStatus);

        securityRepository.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
            verify(securityRepository).updateSensor(sensor);
        });
    }

    @Test
    void ifArmedHomeWhileCameraShowsCat_setAlarmStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}